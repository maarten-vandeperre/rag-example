package com.rag.app.chat.domain.entities;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain entity representing the relationship between a chat answer and the specific document chunks
 * that were used to generate that answer. This enables persistent source detail retrieval.
 */
public final class AnswerSourceReference {
    private final String id;
    private final String answerId;
    private final String documentId;
    private final String chunkId;
    private final String snippetContent;
    private final String snippetContext;
    private final Integer startPosition;
    private final Integer endPosition;
    private final Double relevanceScore;
    private final Integer sourceOrder;
    private final String documentTitle;
    private final String documentFilename;
    private final String documentFileType;
    private final Integer pageNumber;
    private final Integer chunkIndex;
    private final Instant createdAt;

    public AnswerSourceReference(
            String id,
            String answerId,
            String documentId,
            String chunkId,
            String snippetContent,
            String snippetContext,
            Integer startPosition,
            Integer endPosition,
            Double relevanceScore,
            Integer sourceOrder,
            String documentTitle,
            String documentFilename,
            String documentFileType,
            Integer pageNumber,
            Integer chunkIndex,
            Instant createdAt
    ) {
        this.id = Objects.requireNonNull(id, "ID cannot be null");
        this.answerId = Objects.requireNonNull(answerId, "Answer ID cannot be null");
        this.documentId = documentId; // Can be null if document is deleted
        this.chunkId = Objects.requireNonNull(chunkId, "Chunk ID cannot be null");
        this.snippetContent = Objects.requireNonNull(snippetContent, "Snippet content cannot be null");
        this.snippetContext = snippetContext; // Optional
        this.startPosition = startPosition; // Optional
        this.endPosition = endPosition; // Optional
        this.relevanceScore = Objects.requireNonNull(relevanceScore, "Relevance score cannot be null");
        this.sourceOrder = Objects.requireNonNull(sourceOrder, "Source order cannot be null");
        this.documentTitle = documentTitle; // Optional
        this.documentFilename = documentFilename; // Optional
        this.documentFileType = documentFileType; // Optional
        this.pageNumber = pageNumber; // Optional
        this.chunkIndex = chunkIndex; // Optional
        this.createdAt = Objects.requireNonNull(createdAt, "Created at cannot be null");

        validateRelevanceScore(relevanceScore);
        validateSourceOrder(sourceOrder);
        validatePositions(startPosition, endPosition);
    }

    /**
     * Creates a new AnswerSourceReference with required fields only.
     * Use the Builder for optional fields.
     */
    public static AnswerSourceReference create(
            String answerId,
            String documentId,
            String chunkId,
            String snippetContent,
            Double relevanceScore,
            Integer sourceOrder
    ) {
        return new AnswerSourceReference(
                UUID.randomUUID().toString(),
                answerId,
                documentId,
                chunkId,
                snippetContent,
                null, // snippetContext
                null, // startPosition
                null, // endPosition
                relevanceScore,
                sourceOrder,
                null, // documentTitle
                null, // documentFilename
                null, // documentFileType
                null, // pageNumber
                null, // chunkIndex
                Instant.now()
        );
    }

    /**
     * Builder pattern for creating AnswerSourceReference with optional fields.
     */
    public static class Builder {
        private final String answerId;
        private final String documentId;
        private final String chunkId;
        private final String snippetContent;
        private final Double relevanceScore;
        private final Integer sourceOrder;
        
        private String snippetContext;
        private Integer startPosition;
        private Integer endPosition;
        private String documentTitle;
        private String documentFilename;
        private String documentFileType;
        private Integer pageNumber;
        private Integer chunkIndex;

        public Builder(String answerId, String documentId, String chunkId, 
                      String snippetContent, Double relevanceScore, Integer sourceOrder) {
            this.answerId = answerId;
            this.documentId = documentId;
            this.chunkId = chunkId;
            this.snippetContent = snippetContent;
            this.relevanceScore = relevanceScore;
            this.sourceOrder = sourceOrder;
        }

        public Builder withContext(String context) {
            this.snippetContext = context;
            return this;
        }

        public Builder withPositions(Integer start, Integer end) {
            this.startPosition = start;
            this.endPosition = end;
            return this;
        }

        public Builder withDocumentMetadata(String title, String filename, String fileType) {
            this.documentTitle = title;
            this.documentFilename = filename;
            this.documentFileType = fileType;
            return this;
        }

        public Builder withPageInfo(Integer pageNumber, Integer chunkIndex) {
            this.pageNumber = pageNumber;
            this.chunkIndex = chunkIndex;
            return this;
        }

        public AnswerSourceReference build() {
            return new AnswerSourceReference(
                    UUID.randomUUID().toString(),
                    answerId,
                    documentId,
                    chunkId,
                    snippetContent,
                    snippetContext,
                    startPosition,
                    endPosition,
                    relevanceScore,
                    sourceOrder,
                    documentTitle,
                    documentFilename,
                    documentFileType,
                    pageNumber,
                    chunkIndex,
                    Instant.now()
            );
        }
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getAnswerId() {
        return answerId;
    }

    public String getDocumentId() {
        return documentId;
    }

    public String getChunkId() {
        return chunkId;
    }

    public String getSnippetContent() {
        return snippetContent;
    }

    public String getSnippetContext() {
        return snippetContext;
    }

    public Integer getStartPosition() {
        return startPosition;
    }

    public Integer getEndPosition() {
        return endPosition;
    }

    public Double getRelevanceScore() {
        return relevanceScore;
    }

    public Integer getSourceOrder() {
        return sourceOrder;
    }

    public String getDocumentTitle() {
        return documentTitle;
    }

    public String getDocumentFilename() {
        return documentFilename;
    }

    public String getDocumentFileType() {
        return documentFileType;
    }

    public Integer getPageNumber() {
        return pageNumber;
    }

    public Integer getChunkIndex() {
        return chunkIndex;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    // Business methods
    public boolean isHighlyRelevant() {
        return relevanceScore >= 0.8;
    }

    public boolean hasPositionInfo() {
        return startPosition != null && endPosition != null;
    }

    public boolean hasDocumentMetadata() {
        return documentTitle != null || documentFilename != null;
    }

    public boolean hasPageInfo() {
        return pageNumber != null || chunkIndex != null;
    }

    public boolean isDocumentAvailable() {
        return documentId != null;
    }

    // Validation methods
    private void validateRelevanceScore(Double score) {
        if (score < 0.0 || score > 1.0) {
            throw new IllegalArgumentException("Relevance score must be between 0.0 and 1.0");
        }
    }

    private void validateSourceOrder(Integer order) {
        if (order < 0) {
            throw new IllegalArgumentException("Source order must be non-negative");
        }
    }

    private void validatePositions(Integer start, Integer end) {
        if (start != null && end != null && start > end) {
            throw new IllegalArgumentException("Start position must not be greater than end position");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AnswerSourceReference that = (AnswerSourceReference) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "AnswerSourceReference{" +
                "id='" + id + '\'' +
                ", answerId='" + answerId + '\'' +
                ", documentId='" + documentId + '\'' +
                ", chunkId='" + chunkId + '\'' +
                ", relevanceScore=" + relevanceScore +
                ", sourceOrder=" + sourceOrder +
                '}';
    }
}