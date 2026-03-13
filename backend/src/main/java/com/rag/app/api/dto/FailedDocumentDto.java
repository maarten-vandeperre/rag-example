package com.rag.app.api.dto;

import java.time.Instant;

public record FailedDocumentDto(String documentId,
                                String fileName,
                                String uploadedBy,
                                Instant uploadedAt,
                                String failureReason,
                                long fileSize) {
}
