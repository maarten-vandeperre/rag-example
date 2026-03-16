package com.rag.app.integration.api.dto.knowledge;

import java.util.Map;

public record KnowledgeRelationshipDto(String relationshipId,
                                       String fromNodeId,
                                       String toNodeId,
                                       String relationshipType,
                                       Map<String, Object> properties,
                                       KnowledgeDocumentReferenceDto sourceDocument,
                                       double confidence,
                                       String createdAt) {
}
