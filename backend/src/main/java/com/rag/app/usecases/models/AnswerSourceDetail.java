package com.rag.app.usecases.models;

import com.rag.app.domain.valueobjects.SourceSnippet;

import java.util.Objects;
import java.util.UUID;

public record AnswerSourceDetail(String sourceId,
                                 UUID documentId,
                                 String fileName,
                                 String fileType,
                                 SourceSnippet snippet,
                                 AnswerSourceMetadata metadata,
                                 double relevanceScore,
                                 boolean available) {

    public AnswerSourceDetail {
        if (sourceId == null || sourceId.isBlank()) {
            throw new IllegalArgumentException("sourceId must not be null or empty");
        }
        Objects.requireNonNull(documentId, "documentId must not be null");
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("fileName must not be null or empty");
        }
    }
}
