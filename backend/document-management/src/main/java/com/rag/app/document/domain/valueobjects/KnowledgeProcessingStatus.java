package com.rag.app.document.domain.valueobjects;

public enum KnowledgeProcessingStatus {
    NOT_STARTED("Knowledge processing not started"),
    IN_PROGRESS("Knowledge processing in progress"),
    COMPLETED("Knowledge processing completed successfully"),
    COMPLETED_WITH_WARNINGS("Knowledge processing completed with warnings"),
    FAILED("Knowledge processing failed"),
    SKIPPED("Knowledge processing skipped due to insufficient content"),
    DISABLED("Knowledge processing disabled for this document type");

    private final String description;

    KnowledgeProcessingStatus(String description) {
        this.description = description;
    }

    public String description() {
        return description;
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == COMPLETED_WITH_WARNINGS || this == FAILED || this == SKIPPED || this == DISABLED;
    }

    public boolean isSuccessful() {
        return this == COMPLETED || this == COMPLETED_WITH_WARNINGS;
    }
}
