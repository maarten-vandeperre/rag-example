package com.rag.app.chat.usecases;

import com.rag.app.chat.domain.entities.AnswerSourceReference;
import com.rag.app.chat.domain.entities.ChatMessage;
import com.rag.app.chat.domain.services.ChatDomainService;
import com.rag.app.chat.domain.valueobjects.QueryContext;
import com.rag.app.chat.domain.valueobjects.UserRole;
import com.rag.app.chat.interfaces.AnswerSourceReferenceRepository;
import com.rag.app.chat.interfaces.ChatMessageRepository;
import com.rag.app.chat.interfaces.DocumentAccessService;
import com.rag.app.chat.interfaces.SemanticSearch;
import com.rag.app.chat.interfaces.UserContextService;
import com.rag.app.chat.usecases.models.DocumentChunk;
import com.rag.app.chat.usecases.models.DocumentSummary;
import com.rag.app.chat.usecases.models.QueryDocumentsInput;
import com.rag.app.chat.usecases.models.QueryDocumentsOutput;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class QueryDocuments {
    private static final String NO_DOCUMENTS_MESSAGE = "No ready documents available for this query";
    private static final String NO_MATCHES_MESSAGE = "No relevant documents found for the question";

    private final UserContextService userContextService;
    private final DocumentAccessService documentAccessService;
    private final SemanticSearch semanticSearch;
    private final GenerateAnswer generateAnswer;
    private final ChatMessageRepository chatMessageRepository;
    private final AnswerSourceReferenceRepository answerSourceReferenceRepository;
    private final ChatDomainService chatDomainService;
    private final Clock clock;

    public QueryDocuments(UserContextService userContextService,
                          DocumentAccessService documentAccessService,
                          SemanticSearch semanticSearch,
                          GenerateAnswer generateAnswer,
                          ChatMessageRepository chatMessageRepository,
                          AnswerSourceReferenceRepository answerSourceReferenceRepository,
                          ChatDomainService chatDomainService,
                          Clock clock) {
        this.userContextService = Objects.requireNonNull(userContextService, "userContextService must not be null");
        this.documentAccessService = Objects.requireNonNull(documentAccessService, "documentAccessService must not be null");
        this.semanticSearch = Objects.requireNonNull(semanticSearch, "semanticSearch must not be null");
        this.generateAnswer = Objects.requireNonNull(generateAnswer, "generateAnswer must not be null");
        this.chatMessageRepository = Objects.requireNonNull(chatMessageRepository, "chatMessageRepository must not be null");
        this.answerSourceReferenceRepository = Objects.requireNonNull(answerSourceReferenceRepository, "answerSourceReferenceRepository must not be null");
        this.chatDomainService = Objects.requireNonNull(chatDomainService, "chatDomainService must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public QueryDocumentsOutput execute(QueryDocumentsInput input) {
        Objects.requireNonNull(input, "input must not be null");
        if (input.userId() == null || input.userId().isBlank()) {
            throw new IllegalArgumentException("userId must not be null or empty");
        }
        chatDomainService.ensureValidQuestion(input.question());
        if (input.maxResponseTimeMs() <= 0) {
            throw new IllegalArgumentException("maxResponseTimeMs must be positive");
        }

        chatDomainService.ensureActiveUser(userContextService.isActiveUser(input.userId()));
        UserRole userRole = userContextService.getUserRole(input.userId());
        List<DocumentSummary> accessibleDocuments = documentAccessService.getAccessibleDocuments(input.userId(), userRole).stream()
            .filter(DocumentSummary::ready)
            .toList();
        if (accessibleDocuments.isEmpty()) {
            return failure(NO_DOCUMENTS_MESSAGE, 0);
        }

        QueryContext queryContext = new QueryContext(
            input.question(),
            accessibleDocuments.stream().map(DocumentSummary::documentId).toList(),
            input.maxResponseTimeMs()
        );

        long startedAt = clock.millis();
        List<DocumentChunk> chunks = semanticSearch.searchDocuments(queryContext.question(), queryContext.accessibleDocumentIds());
        if (chunks.isEmpty()) {
            return failure(NO_MATCHES_MESSAGE, elapsed(startedAt));
        }

        var generatedAnswer = generateAnswer.execute(queryContext.question(), chunks);
        int responseTimeMs = elapsed(startedAt);
        if (responseTimeMs <= 0) {
            responseTimeMs = 1;
        }
        
        // Create and save the chat message
        ChatMessage message = new ChatMessage(
            UUID.randomUUID(),
            input.userId(),
            input.question(),
            generatedAnswer.answer(),
            generatedAnswer.documentReferences(),
            Instant.now(clock),
            Duration.ofMillis(responseTimeMs)
        );
        ChatMessage savedMessage = chatMessageRepository.save(message);

        // CRITICAL: Persist source references for detailed source retrieval
        try {
            List<AnswerSourceReference> sourceReferences = createSourceReferences(
                savedMessage.messageId().toString(), 
                chunks
            );
            
            for (AnswerSourceReference sourceRef : sourceReferences) {
                answerSourceReferenceRepository.save(sourceRef);
            }
            
        } catch (Exception e) {
            // Log the error but don't fail the entire operation
            // The answer was already saved successfully
            System.err.println("Warning: Failed to persist source references for answer " + 
                savedMessage.messageId() + ": " + e.getMessage());
        }

        if (!chatDomainService.isWithinLimit(message, input.maxResponseTimeMs())) {
            return failure("Response time exceeded limit", responseTimeMs);
        }

        return new QueryDocumentsOutput(
            generatedAnswer.answer(),
            generatedAnswer.documentReferences(),
            responseTimeMs,
            true,
            null
        );
    }

    private QueryDocumentsOutput failure(String errorMessage, int responseTimeMs) {
        return new QueryDocumentsOutput(null, List.of(), responseTimeMs, false, errorMessage);
    }

    private int elapsed(long startedAt) {
        return (int) (clock.millis() - startedAt);
    }
    
    /**
     * Creates AnswerSourceReference entities from the retrieved document chunks.
     * This preserves the chunk-level details needed for source detail retrieval.
     */
    private List<AnswerSourceReference> createSourceReferences(
            String answerId, 
            List<DocumentChunk> retrievedChunks
    ) {
        List<AnswerSourceReference> references = new ArrayList<>();
        
        for (int i = 0; i < retrievedChunks.size(); i++) {
            DocumentChunk chunk = retrievedChunks.get(i);
            
            try {
                AnswerSourceReference.Builder builder = new AnswerSourceReference.Builder(
                    answerId,
                    chunk.documentId().toString(),
                    chunk.chunkId(),
                    chunk.text(), // This is the actual snippet content
                    chunk.relevanceScore(),
                    i // source order
                );
                
                // Add document metadata
                builder.withDocumentMetadata(
                    chunk.documentName(), // Use as title
                    chunk.documentName(), // Use as filename
                    "UNKNOWN" // File type not available in DocumentChunk
                );
                
                // Add chunk-specific metadata
                builder.withPageInfo(null, chunk.chunkIndex()); // Page number not available
                
                // Add context (paragraph reference)
                builder.withContext(chunk.paragraphReference());
                
                references.add(builder.build());
                
            } catch (Exception e) {
                // Log the error but continue with other chunks
                System.err.println("Warning: Failed to create source reference for chunk " + 
                    chunk.chunkId() + ": " + e.getMessage());
            }
        }
        
        return references;
    }
}
