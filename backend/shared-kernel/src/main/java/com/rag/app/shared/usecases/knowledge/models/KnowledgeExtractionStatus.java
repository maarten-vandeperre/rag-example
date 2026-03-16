package com.rag.app.shared.usecases.knowledge.models;

public enum KnowledgeExtractionStatus {
    SUCCESS("Knowledge extraction completed successfully"),
    PARTIAL_SUCCESS("Knowledge extraction completed with warnings"),
    FAILED("Knowledge extraction failed"),
    INSUFFICIENT_CONTENT("Document content insufficient for knowledge extraction"),
    UNSUPPORTED_FORMAT("Document format not supported for knowledge extraction"),
    PROCESSING_ERROR("Error occurred during knowledge processing");

    private final String description;

    KnowledgeExtractionStatus(String description) {
        this.description = description;
    }

    public String description() {
        return description;
    }
}
