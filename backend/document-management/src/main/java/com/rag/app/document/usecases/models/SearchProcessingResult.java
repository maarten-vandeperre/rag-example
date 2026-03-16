package com.rag.app.document.usecases.models;

public record SearchProcessingResult(boolean successful, String errorMessage) {
    public static SearchProcessingResult success() {
        return new SearchProcessingResult(true, null);
    }

    public static SearchProcessingResult failure(String errorMessage) {
        return new SearchProcessingResult(false, errorMessage);
    }

    public boolean isSuccessful() {
        return successful;
    }
}
