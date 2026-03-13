package com.rag.app.api.dto;

import java.time.Instant;

public record DocumentSummaryDto(String documentId,
                                 String fileName,
                                 long fileSize,
                                 String fileType,
                                 String status,
                                 String uploadedBy,
                                 Instant uploadedAt,
                                 Instant lastUpdated) {
}
