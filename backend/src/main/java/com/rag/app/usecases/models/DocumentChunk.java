package com.rag.app.usecases.models;

import java.util.Arrays;
import java.util.UUID;

public record DocumentChunk(String chunkId,
                            UUID documentId,
                             String documentName,
                             int chunkIndex,
                             String paragraphReference,
                             String text,
                             double[] embedding,
                             double relevanceScore) {

    public DocumentChunk {
        if (chunkId == null || chunkId.isBlank()) {
            throw new IllegalArgumentException("chunkId must not be null or empty");
        }
        if (documentId == null) {
            throw new IllegalArgumentException("documentId must not be null");
        }
        if (documentName == null || documentName.isBlank()) {
            throw new IllegalArgumentException("documentName must not be null or empty");
        }
        if (chunkIndex < 0) {
            throw new IllegalArgumentException("chunkIndex must not be negative");
        }
        if (paragraphReference == null || paragraphReference.isBlank()) {
            throw new IllegalArgumentException("paragraphReference must not be null or empty");
        }
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("text must not be null or empty");
        }

        embedding = embedding == null ? new double[0] : Arrays.copyOf(embedding, embedding.length);
    }

    public DocumentChunk(UUID documentId,
                         String documentName,
                         String paragraphReference,
                         String text,
                         double relevanceScore) {
        this(UUID.randomUUID().toString(), documentId, documentName, 0, paragraphReference, text, new double[0], relevanceScore);
    }

    @Override
    public double[] embedding() {
        return Arrays.copyOf(embedding, embedding.length);
    }
}
