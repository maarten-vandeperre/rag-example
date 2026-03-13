package com.rag.app.usecases.models;

import java.time.Instant;

public record ProcessingDocumentInfo(String documentId,
                                     String fileName,
                                     String uploadedBy,
                                     Instant uploadedAt,
                                     Instant processingStartedAt) {
}
