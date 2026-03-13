package com.rag.app.api.dto;

public record DocumentReferenceDto(String documentId,
                                   String documentName,
                                   String paragraphReference,
                                   double relevanceScore) {
}
