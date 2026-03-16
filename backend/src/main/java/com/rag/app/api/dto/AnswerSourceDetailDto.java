package com.rag.app.api.dto;

public record AnswerSourceDetailDto(String sourceId,
                                    String documentId,
                                    String fileName,
                                    String fileType,
                                    SourceSnippetDto snippet,
                                    AnswerSourceMetadataDto metadata,
                                    double relevanceScore,
                                    boolean available) {
}
