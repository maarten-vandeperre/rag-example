package com.rag.app.integration.events.events;

import com.rag.app.document.domain.valueobjects.DocumentStatus;
import com.rag.app.document.domain.valueobjects.KnowledgeProcessingStatus;
import com.rag.app.shared.domain.events.DomainEvent;

import java.util.List;

public final class DocumentProcessedEvent extends DomainEvent {
    private final String documentId;
    private final DocumentStatus status;
    private final String extractedContent;
    private final int extractedTextLength;
    private final KnowledgeProcessingStatus knowledgeProcessingStatus;
    private final List<String> knowledgeProcessingWarnings;
    private final String knowledgeProcessingError;
    private final String associatedGraphId;

    public DocumentProcessedEvent(String documentId,
                                  DocumentStatus status,
                                  String extractedContent,
                                  int extractedTextLength,
                                  KnowledgeProcessingStatus knowledgeProcessingStatus,
                                  List<String> knowledgeProcessingWarnings,
                                  String knowledgeProcessingError,
                                  String associatedGraphId) {
        super("DocumentProcessed");
        this.documentId = documentId;
        this.status = status;
        this.extractedContent = extractedContent;
        this.extractedTextLength = extractedTextLength;
        this.knowledgeProcessingStatus = knowledgeProcessingStatus;
        this.knowledgeProcessingWarnings = knowledgeProcessingWarnings == null ? List.of() : List.copyOf(knowledgeProcessingWarnings);
        this.knowledgeProcessingError = knowledgeProcessingError;
        this.associatedGraphId = associatedGraphId;
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

    public int extractedTextLength() {
        return extractedTextLength;
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

    public String associatedGraphId() {
        return associatedGraphId;
    }
}
