package com.rag.app.integration.events.events;

import com.rag.app.shared.domain.events.DomainEvent;

public final class DocumentUploadedEvent extends DomainEvent {
    private final String documentId;
    private final String fileName;
    private final String uploadedBy;

    public DocumentUploadedEvent(String documentId, String fileName, String uploadedBy) {
        super("DocumentUploaded");
        this.documentId = documentId;
        this.fileName = fileName;
        this.uploadedBy = uploadedBy;
    }

    public String documentId() {
        return documentId;
    }

    public String fileName() {
        return fileName;
    }

    public String uploadedBy() {
        return uploadedBy;
    }
}
