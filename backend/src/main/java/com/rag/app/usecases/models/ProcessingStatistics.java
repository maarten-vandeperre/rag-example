package com.rag.app.usecases.models;

public record ProcessingStatistics(int totalDocuments,
                                   int uploadedCount,
                                   int processingCount,
                                   int readyCount,
                                   int failedCount) {
    public ProcessingStatistics {
        if (totalDocuments < 0 || uploadedCount < 0 || processingCount < 0 || readyCount < 0 || failedCount < 0) {
            throw new IllegalArgumentException("processing statistics counts must not be negative");
        }
    }
}
