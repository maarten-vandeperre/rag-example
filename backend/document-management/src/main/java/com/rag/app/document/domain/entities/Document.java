package com.rag.app.document.domain.entities;

import com.rag.app.document.domain.valueobjects.DocumentMetadata;
import com.rag.app.document.domain.valueobjects.DocumentStatus;
import com.rag.app.document.domain.valueobjects.FileType;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class Document {
    private final UUID documentId;
    private final DocumentMetadata metadata;
    private final String uploadedBy;
    private final Instant uploadedAt;
    private final Instant lastUpdated;
    private final DocumentStatus status;

    public Document(UUID documentId,
                    DocumentMetadata metadata,
                    String uploadedBy,
                    Instant uploadedAt,
                    Instant lastUpdated,
                    DocumentStatus status) {
        this.documentId = Objects.requireNonNull(documentId, "documentId must not be null");
        this.metadata = Objects.requireNonNull(metadata, "metadata must not be null");
        if (uploadedBy == null || uploadedBy.isBlank()) {
            throw new IllegalArgumentException("uploadedBy must not be null or empty");
        }
        this.uploadedBy = uploadedBy;
        this.uploadedAt = Objects.requireNonNull(uploadedAt, "uploadedAt must not be null");
        this.lastUpdated = Objects.requireNonNull(lastUpdated, "lastUpdated must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
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

    public DocumentMetadata metadata() {
        return metadata;
    }

    public Document withStatus(DocumentStatus newStatus, Instant changedAt) {
        return new Document(documentId, metadata, uploadedBy, uploadedAt, changedAt, newStatus);
    }
}
