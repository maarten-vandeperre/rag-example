package com.rag.app.api.dto;

import java.util.List;

public record ChatQueryResponse(String answerId,
                                String answer,
                                List<DocumentReferenceDto> documentReferences,
                                int responseTimeMs,
                                boolean success,
                                String errorMessage) {
    public ChatQueryResponse {
        documentReferences = documentReferences == null ? List.of() : List.copyOf(documentReferences);
    }
}
