package com.rag.app.chat.usecases.models;

import java.util.List;

/**
 * Output model for answer source details retrieval.
 */
public record GetAnswerSourceDetailsOutput(
    String answerId,
    List<SourceDetail> sourceDetails,
    int totalSources,
    int availableSources,
    boolean success,
    String errorMessage
) {
    
    public static GetAnswerSourceDetailsOutput success(
            String answerId,
            List<SourceDetail> sourceDetails,
            int totalSources,
            int availableSources,
            String message
    ) {
        return new GetAnswerSourceDetailsOutput(
            answerId,
            sourceDetails,
            totalSources,
            availableSources,
            true,
            message
        );
    }
    
    public static GetAnswerSourceDetailsOutput notFound(String errorMessage) {
        return new GetAnswerSourceDetailsOutput(
            null,
            List.of(),
            0,
            0,
            false,
            errorMessage
        );
    }
    
    public static GetAnswerSourceDetailsOutput unauthorized(String errorMessage) {
        return new GetAnswerSourceDetailsOutput(
            null,
            List.of(),
            0,
            0,
            false,
            errorMessage
        );
    }
    
    public static GetAnswerSourceDetailsOutput error(String errorMessage) {
        return new GetAnswerSourceDetailsOutput(
            null,
            List.of(),
            0,
            0,
            false,
            errorMessage
        );
    }
}