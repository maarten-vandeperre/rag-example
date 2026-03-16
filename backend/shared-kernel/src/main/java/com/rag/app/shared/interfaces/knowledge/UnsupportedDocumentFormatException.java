package com.rag.app.shared.interfaces.knowledge;

public final class UnsupportedDocumentFormatException extends KnowledgeExtractionException {
    public UnsupportedDocumentFormatException(String message) {
        super(message);
    }
}
