package com.rag.app.usecases.models;

import java.util.UUID;

public record QueryDocumentsInput(UUID userId, String question, int maxResponseTimeMs) {
    public static final int DEFAULT_MAX_RESPONSE_TIME_MS = 20_000;

    public QueryDocumentsInput(UUID userId, String question) {
        this(userId, question, DEFAULT_MAX_RESPONSE_TIME_MS);
    }
}
