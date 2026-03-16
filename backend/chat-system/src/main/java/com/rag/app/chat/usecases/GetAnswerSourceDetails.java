package com.rag.app.chat.usecases;

import com.rag.app.chat.domain.entities.AnswerSourceReference;
import com.rag.app.chat.domain.entities.ChatMessage;
import com.rag.app.chat.interfaces.AnswerSourceReferenceRepository;
import com.rag.app.chat.interfaces.ChatMessageRepository;
import com.rag.app.chat.usecases.models.GetAnswerSourceDetailsInput;
import com.rag.app.chat.usecases.models.GetAnswerSourceDetailsOutput;
import com.rag.app.chat.usecases.models.SourceDetail;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Use case for retrieving detailed source information for a chat answer.
 * This replaces the in-memory chunk storage approach with persistent data retrieval.
 */
public final class GetAnswerSourceDetails {
    
    private final ChatMessageRepository chatMessageRepository;
    private final AnswerSourceReferenceRepository answerSourceReferenceRepository;

    public GetAnswerSourceDetails(
            ChatMessageRepository chatMessageRepository,
            AnswerSourceReferenceRepository answerSourceReferenceRepository
    ) {
        this.chatMessageRepository = Objects.requireNonNull(chatMessageRepository, "chatMessageRepository must not be null");
        this.answerSourceReferenceRepository = Objects.requireNonNull(answerSourceReferenceRepository, "answerSourceReferenceRepository must not be null");
    }

    public GetAnswerSourceDetailsOutput execute(GetAnswerSourceDetailsInput input) {
        Objects.requireNonNull(input, "input must not be null");
        
        UUID answerId;
        try {
            answerId = UUID.fromString(input.answerId());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid answer ID format: " + input.answerId());
        }
        
        // 1. Validate answer exists and user has access
        Optional<ChatMessage> answer = chatMessageRepository.findById(answerId);
        if (answer.isEmpty()) {
            return GetAnswerSourceDetailsOutput.notFound("Answer not found: " + input.answerId());
        }
        
        ChatMessage chatMessage = answer.get();
        if (!chatMessage.userId().equals(input.userId())) {
            return GetAnswerSourceDetailsOutput.unauthorized("User does not have access to this answer");
        }
        
        // 2. Retrieve persisted source references
        List<AnswerSourceReference> sourceReferences = answerSourceReferenceRepository
            .findByAnswerIdOrderBySourceOrder(input.answerId());
        
        if (sourceReferences.isEmpty()) {
            return GetAnswerSourceDetailsOutput.success(
                input.answerId(),
                List.of(),
                0,
                0,
                "No source references found for this answer"
            );
        }
        
        // 3. Convert to response format with availability checking
        List<SourceDetail> sourceDetails = sourceReferences.stream()
            .map(this::convertToSourceDetail)
            .collect(Collectors.toList());
        
        // 4. Count available vs unavailable sources
        long availableCount = sourceDetails.stream()
            .mapToLong(detail -> detail.isAvailable() ? 1 : 0)
            .sum();
        
        return GetAnswerSourceDetailsOutput.success(
            input.answerId(),
            sourceDetails,
            sourceDetails.size(),
            (int) availableCount,
            null
        );
    }
    
    /**
     * Converts an AnswerSourceReference to a SourceDetail for the response.
     */
    private SourceDetail convertToSourceDetail(AnswerSourceReference reference) {
        try {
            // Check if document is still available (document_id not null)
            boolean documentAvailable = reference.isDocumentAvailable();
            
            if (!documentAvailable) {
                return createUnavailableSourceDetail(reference, "Document no longer available");
            }
            
            // Create source detail from persisted data
            return SourceDetail.available(
                reference.getChunkId(),
                reference.getDocumentId(),
                reference.getDocumentFilename() != null ? reference.getDocumentFilename() : "Unknown",
                reference.getDocumentFileType() != null ? reference.getDocumentFileType() : "UNKNOWN",
                reference.getSnippetContent(),
                reference.getSnippetContext(),
                reference.getStartPosition(),
                reference.getEndPosition(),
                reference.getDocumentTitle(),
                reference.getPageNumber(),
                reference.getChunkIndex(),
                reference.getRelevanceScore()
            );
            
        } catch (Exception e) {
            return createUnavailableSourceDetail(reference, "Error loading source details: " + e.getMessage());
        }
    }
    
    /**
     * Creates a SourceDetail for an unavailable source.
     */
    private SourceDetail createUnavailableSourceDetail(AnswerSourceReference reference, String reason) {
        return SourceDetail.unavailable(
            reference.getChunkId(),
            reference.getDocumentId(),
            reference.getDocumentFilename() != null ? reference.getDocumentFilename() : "Unknown",
            reference.getDocumentFileType() != null ? reference.getDocumentFileType() : "UNKNOWN",
            reference.getRelevanceScore(),
            reason
        );
    }
}