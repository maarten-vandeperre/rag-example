package com.rag.app.usecases.models;

import com.rag.app.domain.valueobjects.DocumentReference;

import java.util.List;

public record QueryDocumentsOutput(String answer,
                                    List<DocumentReference> documentReferences,
                                   List<DocumentChunk> sourceChunks,
                                    int responseTimeMs,
                                    boolean success,
                                    String errorMessage) {
    public QueryDocumentsOutput {
        documentReferences = documentReferences == null ? List.of() : List.copyOf(documentReferences);
        sourceChunks = sourceChunks == null ? List.of() : List.copyOf(sourceChunks);
    }
}
