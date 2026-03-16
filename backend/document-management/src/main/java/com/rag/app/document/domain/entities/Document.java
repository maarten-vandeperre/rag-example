package com.rag.app.document.domain.entities;

import com.rag.app.document.domain.valueobjects.DocumentMetadata;
import com.rag.app.document.domain.valueobjects.DocumentStatus;
import com.rag.app.document.domain.valueobjects.FileType;
import com.rag.app.document.domain.valueobjects.KnowledgeProcessingStatus;
import com.rag.app.shared.domain.knowledge.valueobjects.GraphId;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class Document {
    private final UUID documentId;
    private final DocumentMetadata metadata;
    private final String uploadedBy;
    private final Instant uploadedAt;
    private final Instant lastUpdated;
    private final DocumentStatus status;
    private final String failureReason;
    private final Instant processingStartedAt;
    private final KnowledgeProcessingStatus knowledgeProcessingStatus;
    private final List<String> knowledgeProcessingWarnings;
    private final String knowledgeProcessingError;
    private final Instant knowledgeProcessingStartedAt;
    private final Instant knowledgeProcessingCompletedAt;
    private final GraphId associatedGraphId;

    public Document(UUID documentId,
                    DocumentMetadata metadata,
                    String uploadedBy,
                    Instant uploadedAt,
                    Instant lastUpdated,
                    DocumentStatus status) {
        this(documentId, metadata, uploadedBy, uploadedAt, lastUpdated, status, null, null,
            KnowledgeProcessingStatus.NOT_STARTED, List.of(), null, null, null, null);
    }

    public Document(UUID documentId,
                    DocumentMetadata metadata,
                    String uploadedBy,
                    Instant uploadedAt,
                    Instant lastUpdated,
                    DocumentStatus status,
                    String failureReason,
                    Instant processingStartedAt,
                    KnowledgeProcessingStatus knowledgeProcessingStatus,
                    List<String> knowledgeProcessingWarnings,
                    String knowledgeProcessingError,
                    Instant knowledgeProcessingStartedAt,
                    Instant knowledgeProcessingCompletedAt,
                    GraphId associatedGraphId) {
        this.documentId = Objects.requireNonNull(documentId, "documentId must not be null");
        this.metadata = Objects.requireNonNull(metadata, "metadata must not be null");
        if (uploadedBy == null || uploadedBy.isBlank()) {
            throw new IllegalArgumentException("uploadedBy must not be null or empty");
        }
        this.uploadedBy = uploadedBy;
        this.uploadedAt = Objects.requireNonNull(uploadedAt, "uploadedAt must not be null");
        this.lastUpdated = Objects.requireNonNull(lastUpdated, "lastUpdated must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.failureReason = failureReason == null || failureReason.isBlank() ? null : failureReason;
        this.processingStartedAt = processingStartedAt;
        this.knowledgeProcessingStatus = Objects.requireNonNull(knowledgeProcessingStatus, "knowledgeProcessingStatus must not be null");
        this.knowledgeProcessingWarnings = List.copyOf(Objects.requireNonNull(knowledgeProcessingWarnings, "knowledgeProcessingWarnings must not be null"));
        this.knowledgeProcessingError = knowledgeProcessingError == null || knowledgeProcessingError.isBlank() ? null : knowledgeProcessingError;
        this.knowledgeProcessingStartedAt = knowledgeProcessingStartedAt;
        this.knowledgeProcessingCompletedAt = knowledgeProcessingCompletedAt;
        this.associatedGraphId = associatedGraphId;
    }

    public UUID documentId() {
        return documentId;
    }

    public String fileName() {
        return metadata.fileName();
    }

    public long fileSize() {
        return metadata.fileSize();
    }

    public FileType fileType() {
        return metadata.fileType();
    }

    public String contentHash() {
        return metadata.contentHash();
    }

    public String uploadedBy() {
        return uploadedBy;
    }

    public Instant uploadedAt() {
        return uploadedAt;
    }

    public Instant lastUpdated() {
        return lastUpdated;
    }

    public DocumentStatus status() {
        return status;
    }

    public String failureReason() {
        return failureReason;
    }

    public Instant processingStartedAt() {
        return processingStartedAt;
    }

    public KnowledgeProcessingStatus knowledgeProcessingStatus() {
        return knowledgeProcessingStatus;
    }

    public List<String> knowledgeProcessingWarnings() {
        return knowledgeProcessingWarnings;
    }

    public String knowledgeProcessingError() {
        return knowledgeProcessingError;
    }

    public Instant knowledgeProcessingStartedAt() {
        return knowledgeProcessingStartedAt;
    }

    public Instant knowledgeProcessingCompletedAt() {
        return knowledgeProcessingCompletedAt;
    }

    public GraphId associatedGraphId() {
        return associatedGraphId;
    }

    public DocumentMetadata metadata() {
        return metadata;
    }

    public Document withStatus(DocumentStatus newStatus, Instant changedAt) {
        return new Document(documentId, metadata, uploadedBy, uploadedAt, changedAt, newStatus, failureReason, processingStartedAt,
            knowledgeProcessingStatus, knowledgeProcessingWarnings, knowledgeProcessingError,
            knowledgeProcessingStartedAt, knowledgeProcessingCompletedAt, associatedGraphId);
    }

    public Document withProcessingStarted(Instant startedAt) {
        return new Document(documentId, metadata, uploadedBy, uploadedAt, startedAt, DocumentStatus.PROCESSING, failureReason, startedAt,
            knowledgeProcessingStatus, knowledgeProcessingWarnings, knowledgeProcessingError,
            knowledgeProcessingStartedAt, knowledgeProcessingCompletedAt, associatedGraphId);
    }

    public Document withFailureReason(String newFailureReason, Instant changedAt) {
        return new Document(documentId, metadata, uploadedBy, uploadedAt, changedAt, status, newFailureReason, processingStartedAt,
            knowledgeProcessingStatus, knowledgeProcessingWarnings, knowledgeProcessingError,
            knowledgeProcessingStartedAt, knowledgeProcessingCompletedAt, associatedGraphId);
    }

    public Document withKnowledgeProcessingStarted(Instant startedAt) {
        return new Document(documentId, metadata, uploadedBy, uploadedAt, startedAt, status, failureReason, processingStartedAt,
            KnowledgeProcessingStatus.IN_PROGRESS, List.of(), null, startedAt, null, null);
    }

    public Document withKnowledgeProcessingCompleted(GraphId graphId, List<String> warnings, Instant completedAt) {
        KnowledgeProcessingStatus completedStatus = warnings == null || warnings.isEmpty()
            ? KnowledgeProcessingStatus.COMPLETED
            : KnowledgeProcessingStatus.COMPLETED_WITH_WARNINGS;
        return new Document(documentId, metadata, uploadedBy, uploadedAt, completedAt, status, failureReason, processingStartedAt,
            completedStatus, warnings == null ? List.of() : warnings, null,
            knowledgeProcessingStartedAt, completedAt, graphId);
    }

    public Document withKnowledgeProcessingFailed(String error, List<String> warnings, Instant completedAt) {
        return new Document(documentId, metadata, uploadedBy, uploadedAt, completedAt, status, failureReason, processingStartedAt,
            KnowledgeProcessingStatus.FAILED, warnings == null ? List.of() : warnings, error,
            knowledgeProcessingStartedAt, completedAt, associatedGraphId);
    }

    public Document withKnowledgeProcessingSkipped(KnowledgeProcessingStatus skippedStatus, List<String> warnings, String error, Instant completedAt) {
        if (skippedStatus != KnowledgeProcessingStatus.SKIPPED && skippedStatus != KnowledgeProcessingStatus.DISABLED) {
            throw new IllegalArgumentException("skippedStatus must be SKIPPED or DISABLED");
        }
        return new Document(documentId, metadata, uploadedBy, uploadedAt, completedAt, status, failureReason, processingStartedAt,
            skippedStatus, warnings == null ? List.of() : warnings, error,
            knowledgeProcessingStartedAt, completedAt, null);
    }

    public boolean isKnowledgeProcessingComplete() {
        return knowledgeProcessingStatus.isTerminal();
    }

    public boolean hasKnowledgeProcessingWarnings() {
        return !knowledgeProcessingWarnings.isEmpty();
    }
}
