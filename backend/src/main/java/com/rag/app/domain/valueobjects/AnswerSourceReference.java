package com.rag.app.domain.valueobjects;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record AnswerSourceReference(UUID referenceId,
                                    UUID answerId,
                                    UUID documentId,
                                    String chunkId,
                                    String snippetContent,
                                    String snippetContext,
                                    Integer startPosition,
                                    Integer endPosition,
                                    double relevanceScore,
                                    int sourceOrder,
                                    String documentTitle,
                                    String documentFilename,
                                    String documentFileType,
                                    Integer pageNumber,
                                    Integer chunkIndex,
                                    Instant createdAt) {

    public AnswerSourceReference {
        Objects.requireNonNull(referenceId, "referenceId must not be null");
        Objects.requireNonNull(answerId, "answerId must not be null");
        Objects.requireNonNull(documentId, "documentId must not be null");
        if (chunkId == null || chunkId.isBlank()) {
            throw new IllegalArgumentException("chunkId must not be null or empty");
        }
        if (snippetContent == null || snippetContent.isBlank()) {
            throw new IllegalArgumentException("snippetContent must not be null or empty");
        }
        if (relevanceScore < 0.0d) {
            throw new IllegalArgumentException("relevanceScore must not be negative");
        }
        if (sourceOrder < 0) {
            throw new IllegalArgumentException("sourceOrder must not be negative");
        }
        if (startPosition != null && startPosition < 0) {
            throw new IllegalArgumentException("startPosition must not be negative");
        }
        if (endPosition != null && endPosition < 0) {
            throw new IllegalArgumentException("endPosition must not be negative");
        }
        if (startPosition != null && endPosition != null && endPosition < startPosition) {
            throw new IllegalArgumentException("endPosition must be greater than or equal to startPosition");
        }
        createdAt = createdAt == null ? Instant.now() : createdAt;
    }

    public static AnswerSourceReference fromChunk(UUID answerId, com.rag.app.usecases.models.DocumentChunk chunk, int sourceOrder) {
        return new AnswerSourceReference(
            UUID.randomUUID(),
            answerId,
            chunk.documentId(),
            chunk.chunkId(),
            chunk.text(),
            chunk.text(),
            0,
            chunk.text().length(),
            chunk.relevanceScore(),
            sourceOrder,
            chunk.documentName(),
            chunk.documentName(),
            null,
            null,
            chunk.chunkIndex(),
            Instant.now()
        );
    }
}
