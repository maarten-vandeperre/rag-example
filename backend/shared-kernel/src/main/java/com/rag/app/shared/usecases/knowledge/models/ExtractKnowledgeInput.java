package com.rag.app.shared.usecases.knowledge.models;

import com.rag.app.shared.domain.exceptions.ValidationException;

import java.util.Map;
import java.util.Objects;

public record ExtractKnowledgeInput(String documentId,
                                    String documentContent,
                                    String documentTitle,
                                    String documentType,
                                    Map<String, Object> extractionOptions) {
    public static final int MINIMUM_CONTENT_LENGTH = 100;

    public ExtractKnowledgeInput {
        if (documentId == null || documentId.isBlank()) {
            throw new ValidationException("documentId cannot be null or blank");
        }
        if (documentContent == null || documentContent.isBlank()) {
            throw new ValidationException("documentContent cannot be null or blank");
        }
        if (documentTitle == null || documentTitle.isBlank()) {
            throw new ValidationException("documentTitle cannot be null or blank");
        }
        if (documentType == null || documentType.isBlank()) {
            throw new ValidationException("documentType cannot be null or blank");
        }
        extractionOptions = Map.copyOf(Objects.requireNonNull(extractionOptions, "extractionOptions cannot be null"));
        documentId = documentId.trim();
        documentContent = documentContent.trim();
        documentTitle = documentTitle.trim();
        documentType = documentType.trim();
    }

    public boolean hasMinimumContentLength() {
        return documentContent.length() >= MINIMUM_CONTENT_LENGTH;
    }

    public boolean isExtractionEnabled(String extractionType) {
        return Boolean.TRUE.equals(extractionOptions.getOrDefault(extractionType + "_enabled", Boolean.TRUE));
    }
}
