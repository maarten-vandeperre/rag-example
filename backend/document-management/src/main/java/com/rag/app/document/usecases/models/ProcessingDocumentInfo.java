package com.rag.app.document.usecases.models;

import java.time.Instant;

public record ProcessingDocumentInfo(String documentId,
                                     String fileName,
                                     String uploadedBy,
                                     Instant uploadedAt,
                                     Instant processingStartedAt) {
}
