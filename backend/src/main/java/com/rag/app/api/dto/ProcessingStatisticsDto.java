package com.rag.app.api.dto;

public record ProcessingStatisticsDto(int totalDocuments,
                                      int uploadedCount,
                                      int processingCount,
                                      int readyCount,
                                      int failedCount) {
}
