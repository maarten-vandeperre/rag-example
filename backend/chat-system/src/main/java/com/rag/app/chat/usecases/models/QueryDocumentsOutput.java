package com.rag.app.chat.usecases.models;

import com.rag.app.chat.domain.valueobjects.DocumentReference;

import java.util.List;

public record QueryDocumentsOutput(String answer,
                                   List<DocumentReference> documentReferences,
                                   int responseTimeMs,
                                   boolean success,
                                   String errorMessage) {
}
