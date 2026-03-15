package com.rag.app.integration.events.events;

import com.rag.app.document.domain.valueobjects.DocumentStatus;
import com.rag.app.shared.domain.events.DomainEvent;

public final class DocumentProcessedEvent extends DomainEvent {
    private final String documentId;
    private final DocumentStatus status;
    private final String extractedContent;

    public DocumentProcessedEvent(String documentId, DocumentStatus status, String extractedContent) {
        super("DocumentProcessed");
        this.documentId = documentId;
        this.status = status;
        this.extractedContent = extractedContent;
    }

    public String documentId() {
        return documentId;
    }

    public DocumentStatus status() {
        return status;
    }

    public String extractedContent() {
        return extractedContent;
    }
}
