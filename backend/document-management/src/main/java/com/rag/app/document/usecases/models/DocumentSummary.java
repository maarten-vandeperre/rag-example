package com.rag.app.document.usecases.models;

import com.rag.app.document.domain.valueobjects.DocumentStatus;
import com.rag.app.document.domain.valueobjects.FileType;

import java.time.Instant;
import java.util.UUID;

public record DocumentSummary(UUID documentId,
                              String fileName,
                              long fileSize,
                              FileType fileType,
                              DocumentStatus status,
                              String uploadedBy,
                              Instant uploadedAt,
                              Instant lastUpdated) {
}
