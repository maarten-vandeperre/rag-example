package com.rag.app.chat.usecases.models;

public record QueryDocumentsInput(String userId, String question, int maxResponseTimeMs) {
    public static final int DEFAULT_MAX_RESPONSE_TIME_MS = 20_000;

    public QueryDocumentsInput(String userId, String question) {
        this(userId, question, DEFAULT_MAX_RESPONSE_TIME_MS);
    }
}
