package com.rag.app.document.usecases.models;

import com.rag.app.document.domain.valueobjects.DocumentStatus;

import java.util.UUID;

public record UploadDocumentOutput(UUID documentId, DocumentStatus status, String message) {
}
