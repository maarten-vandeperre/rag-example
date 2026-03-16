package com.rag.app.integration.api.dto;

import java.util.List;

/**
 * API response for answer source details.
 */
public record AnswerSourceDetailsResponse(
    String answerId,
    List<SourceDetailDto> sources,
    int totalSources,
    int availableSources,
    String message
) {
    
    public static AnswerSourceDetailsResponse success(
            String answerId,
            List<SourceDetailDto> sources,
            int totalSources,
            int availableSources,
            String message
    ) {
        return new AnswerSourceDetailsResponse(
            answerId,
            sources,
            totalSources,
            availableSources,
            message
        );
    }
    
    public static AnswerSourceDetailsResponse empty(String answerId, String message) {
        return new AnswerSourceDetailsResponse(
            answerId,
            List.of(),
            0,
            0,
            message
        );
    }
}