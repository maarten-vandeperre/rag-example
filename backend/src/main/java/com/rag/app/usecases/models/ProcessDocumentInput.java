package com.rag.app.usecases.models;

import java.util.UUID;

public record ProcessDocumentInput(UUID documentId, byte[] fileContent) {
}
