package com.rag.app.shared.domain.knowledge.valueobjects;

import com.rag.app.shared.domain.exceptions.ValidationException;

import java.util.UUID;

public record DocumentReference(UUID documentId, String documentName, String sectionReference, double relevanceScore) {
    public DocumentReference {
        if (documentId == null) {
            throw new ValidationException("documentId cannot be null");
        }
        if (documentName == null || documentName.isBlank()) {
            throw new ValidationException("documentName cannot be null or blank");
        }
        if (sectionReference != null && sectionReference.isBlank()) {
            throw new ValidationException("sectionReference cannot be blank when provided");
        }
        if (relevanceScore < 0.0d || relevanceScore > 1.0d) {
            throw new ValidationException("relevanceScore must be between 0.0 and 1.0");
        }

        documentName = documentName.trim();
        sectionReference = sectionReference == null ? null : sectionReference.trim();
    }

    public boolean isHighlyRelevant() {
        return relevanceScore >= 0.8d;
    }
}
