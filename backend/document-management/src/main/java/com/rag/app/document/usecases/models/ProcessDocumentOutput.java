package com.rag.app.document.usecases.models;

import com.rag.app.document.domain.valueobjects.DocumentStatus;

import java.util.UUID;

public record ProcessDocumentOutput(UUID documentId, DocumentStatus finalStatus, int extractedTextLength, String errorMessage) {
}
