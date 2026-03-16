package com.rag.app.shared.usecases.knowledge.models;

import com.rag.app.shared.domain.exceptions.ValidationException;

public record ValidateKnowledgeQualityInput(String documentContent, String documentType) {
    public ValidateKnowledgeQualityInput {
        if (documentContent == null || documentContent.isBlank()) {
            throw new ValidationException("documentContent cannot be null or blank");
        }
        if (documentType == null || documentType.isBlank()) {
            throw new ValidationException("documentType cannot be null or blank");
        }
        documentContent = documentContent.trim();
        documentType = documentType.trim();
    }
}
