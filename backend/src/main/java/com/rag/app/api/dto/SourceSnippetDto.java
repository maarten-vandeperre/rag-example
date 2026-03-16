package com.rag.app.api.dto;

public record SourceSnippetDto(String content,
                               int startPosition,
                               int endPosition,
                               String context) {
}
