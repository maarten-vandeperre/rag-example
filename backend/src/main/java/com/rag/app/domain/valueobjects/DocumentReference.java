package com.rag.app.domain.valueobjects;

import java.util.UUID;

public record DocumentReference(UUID documentId, String documentName, String paragraphReference, double relevanceScore) {

    public DocumentReference {
        if (documentId == null) {
            throw new IllegalArgumentException("documentId must not be null");
        }
        if (documentName == null || documentName.isBlank()) {
            throw new IllegalArgumentException("documentName must not be null or empty");
        }
    }
}
