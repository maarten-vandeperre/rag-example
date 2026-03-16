package com.rag.app.domain.valueobjects;

public record SourceSnippet(String content,
                            int startPosition,
                            int endPosition,
                            String context) {

    public SourceSnippet {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("content must not be null or empty");
        }
        if (startPosition < 0) {
            throw new IllegalArgumentException("startPosition must not be negative");
        }
        if (endPosition < startPosition) {
            throw new IllegalArgumentException("endPosition must be greater than or equal to startPosition");
        }
        context = context == null ? "" : context;
    }
}
