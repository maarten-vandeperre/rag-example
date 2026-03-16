package com.rag.app.shared.interfaces.knowledge;

public class KnowledgeExtractionException extends RuntimeException {
    public KnowledgeExtractionException(String message) {
        super(message);
    }

    public KnowledgeExtractionException(String message, Throwable cause) {
        super(message, cause);
    }
}
