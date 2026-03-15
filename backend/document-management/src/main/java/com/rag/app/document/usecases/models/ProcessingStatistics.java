package com.rag.app.document.usecases.models;

public record ProcessingStatistics(int totalDocuments,
                                   int uploadedCount,
                                   int processingCount,
                                   int readyCount,
                                   int failedCount) {
}
