package com.rag.app.chat.usecases.models;

/**
 * Detailed information about a source used to generate a chat answer.
 */
public record SourceDetail(
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
    public SourceDetail {
        if (chunkId == null || chunkId.isBlank()) {
            throw new IllegalArgumentException("chunkId must not be null or blank");
        }
        if (relevanceScore != null && (relevanceScore < 0.0 || relevanceScore > 1.0)) {
            throw new IllegalArgumentException("relevanceScore must be between 0.0 and 1.0");
        }
    }
    
    /**
     * Creates a SourceDetail for an available source.
     */
    public static SourceDetail available(
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
            Double relevanceScore
    ) {
        return new SourceDetail(
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
            true,
            null
        );
    }
    
    /**
     * Creates a SourceDetail for an unavailable source.
     */
    public static SourceDetail unavailable(
            String chunkId,
            String documentId,
            String documentName,
            String documentType,
            Double relevanceScore,
            String errorMessage
    ) {
        return new SourceDetail(
            chunkId,
            documentId,
            documentName,
            documentType,
            null, // no snippet content
            null, // no context
            null, // no start position
            null, // no end position
            null, // no document title
            null, // no page number
            null, // no chunk index
            relevanceScore,
            false,
            errorMessage
        );
    }
    
    public boolean hasPositionInfo() {
        return startPosition != null && endPosition != null;
    }
    
    public boolean hasPageInfo() {
        return pageNumber != null;
    }
    
    public boolean hasSnippetContent() {
        return snippetContent != null && !snippetContent.isBlank();
    }
}