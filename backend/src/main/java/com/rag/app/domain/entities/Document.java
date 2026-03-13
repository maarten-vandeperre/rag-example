package com.rag.app.domain.entities;

import com.rag.app.domain.valueobjects.DocumentMetadata;
import com.rag.app.domain.valueobjects.DocumentStatus;
import com.rag.app.domain.valueobjects.FileType;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class Document {
    private final UUID documentId;
    private final DocumentMetadata metadata;
    private final String uploadedBy;
    private final Instant uploadedAt;
    private final DocumentStatus status;

    public Document(UUID documentId,
                    DocumentMetadata metadata,
                    String uploadedBy,
                    Instant uploadedAt,
                    DocumentStatus status) {
        this.documentId = Objects.requireNonNull(documentId, "documentId must not be null");
        this.metadata = Objects.requireNonNull(metadata, "metadata must not be null");

        if (uploadedBy == null || uploadedBy.isBlank()) {
            throw new IllegalArgumentException("uploadedBy must not be null or empty");
        }

        this.uploadedBy = uploadedBy;
        this.uploadedAt = Objects.requireNonNull(uploadedAt, "uploadedAt must not be null");
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

    public String uploadedBy() {
        return uploadedBy;
    }

    public Instant uploadedAt() {
        return uploadedAt;
    }

    public DocumentStatus status() {
        return status;
    }

    public String contentHash() {
        return metadata.contentHash();
    }

    public DocumentMetadata metadata() {
        return metadata;
    }

    public Document withStatus(DocumentStatus newStatus) {
        return new Document(documentId, metadata, uploadedBy, uploadedAt, newStatus);
    }
}
