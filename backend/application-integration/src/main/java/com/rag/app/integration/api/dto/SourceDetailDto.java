package com.rag.app.integration.api.dto;

/**
 * DTO for source detail information in API responses.
 */
public record SourceDetailDto(
    String chunkId,
    String documentId,
    String documentName,
    String documentType,
    String snippetContent,
    String snippetContext,
    Integer startPosition,
    Integer endPosition,
    String documentTitle,
    Integer pageNumber,
    Integer chunkIndex,
    Double relevanceScore,
    boolean isAvailable,
    String errorMessage
) {
    
    public static SourceDetailDto fromDomain(
            String chunkId,
            String documentId,
            String documentName,
            String documentType,
            String snippetContent,
            String snippetContext,
            Integer startPosition,
            Integer endPosition,
            String documentTitle,
            Integer pageNumber,
            Integer chunkIndex,
            Double relevanceScore,
            boolean isAvailable,
            String errorMessage
    ) {
        return new SourceDetailDto(
            chunkId,
            documentId,
            documentName,
            documentType,
            snippetContent,
            snippetContext,
            startPosition,
            endPosition,
            documentTitle,
            pageNumber,
            chunkIndex,
            relevanceScore,
            isAvailable,
            errorMessage
        );
    }
}