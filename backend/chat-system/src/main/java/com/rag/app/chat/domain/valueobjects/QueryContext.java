package com.rag.app.chat.domain.valueobjects;

import java.util.List;
import java.util.Objects;

public record QueryContext(String question, List<String> accessibleDocumentIds, int maxResponseTimeMs) {
    public QueryContext {
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("question must not be null or empty");
        }
        accessibleDocumentIds = List.copyOf(Objects.requireNonNull(accessibleDocumentIds, "accessibleDocumentIds must not be null"));
        if (maxResponseTimeMs <= 0) {
            throw new IllegalArgumentException("maxResponseTimeMs must be positive");
        }
    }
}
