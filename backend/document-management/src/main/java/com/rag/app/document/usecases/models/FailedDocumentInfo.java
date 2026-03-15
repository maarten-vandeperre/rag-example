package com.rag.app.document.usecases.models;

import java.time.Instant;

public record FailedDocumentInfo(String documentId,
                                 String fileName,
                                 String uploadedBy,
                                 Instant uploadedAt,
                                 String failureReason,
                                 long fileSize) {
}
