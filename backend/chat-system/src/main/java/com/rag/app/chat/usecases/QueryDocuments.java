package com.rag.app.chat.usecases;

import com.rag.app.chat.domain.entities.ChatMessage;
import com.rag.app.chat.domain.services.ChatDomainService;
import com.rag.app.chat.domain.valueobjects.QueryContext;
import com.rag.app.chat.domain.valueobjects.UserRole;
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
    private final ChatDomainService chatDomainService;
    private final Clock clock;

    public QueryDocuments(UserContextService userContextService,
                          DocumentAccessService documentAccessService,
                          SemanticSearch semanticSearch,
                          GenerateAnswer generateAnswer,
                          ChatMessageRepository chatMessageRepository,
                          ChatDomainService chatDomainService,
                          Clock clock) {
        this.userContextService = Objects.requireNonNull(userContextService, "userContextService must not be null");
        this.documentAccessService = Objects.requireNonNull(documentAccessService, "documentAccessService must not be null");
        this.semanticSearch = Objects.requireNonNull(semanticSearch, "semanticSearch must not be null");
        this.generateAnswer = Objects.requireNonNull(generateAnswer, "generateAnswer must not be null");
        this.chatMessageRepository = Objects.requireNonNull(chatMessageRepository, "chatMessageRepository must not be null");
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
        ChatMessage message = new ChatMessage(
            UUID.randomUUID(),
            input.userId(),
            input.question(),
            generatedAnswer.answer(),
            generatedAnswer.documentReferences(),
            Instant.now(clock),
            Duration.ofMillis(responseTimeMs)
        );
        chatMessageRepository.save(message);

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
}
