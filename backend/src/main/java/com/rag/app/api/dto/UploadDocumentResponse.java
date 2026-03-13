package com.rag.app.api.dto;

import java.time.Instant;

public record UploadDocumentResponse(String documentId,
                                     String fileName,
                                     String status,
                                     String message,
                                     Instant uploadedAt) {
}
