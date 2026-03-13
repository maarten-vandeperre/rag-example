package com.rag.app.api.dto;

public record ChatQueryRequest(String question, Integer maxResponseTimeMs) {
    public static final int DEFAULT_MAX_RESPONSE_TIME_MS = 20_000;

    public int resolvedMaxResponseTimeMs() {
        return maxResponseTimeMs == null ? DEFAULT_MAX_RESPONSE_TIME_MS : maxResponseTimeMs;
    }
}
