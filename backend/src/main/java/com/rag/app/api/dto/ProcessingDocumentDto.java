package com.rag.app.api.dto;

import java.time.Instant;

public record ProcessingDocumentDto(String documentId,
                                    String fileName,
                                    String uploadedBy,
                                    Instant uploadedAt,
                                    Instant processingStartedAt) {
}
