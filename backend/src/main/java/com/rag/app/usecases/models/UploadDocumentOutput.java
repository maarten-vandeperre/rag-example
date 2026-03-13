package com.rag.app.usecases.models;

import com.rag.app.domain.valueobjects.DocumentStatus;

import java.util.UUID;

public record UploadDocumentOutput(UUID documentId, DocumentStatus status, String message) {
}
