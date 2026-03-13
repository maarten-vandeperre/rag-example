package com.rag.app.usecases.models;

import com.rag.app.domain.valueobjects.DocumentStatus;

import java.util.UUID;

public record ProcessDocumentOutput(UUID documentId, DocumentStatus finalStatus, int extractedTextLength, String errorMessage) {
}
