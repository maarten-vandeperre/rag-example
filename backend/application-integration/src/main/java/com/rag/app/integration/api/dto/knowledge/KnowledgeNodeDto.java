package com.rag.app.integration.api.dto.knowledge;

import java.util.Map;

public record KnowledgeNodeDto(String nodeId,
                               String label,
                               String nodeType,
                               Map<String, Object> properties,
                               KnowledgeDocumentReferenceDto sourceDocument,
                               double confidence,
                               String createdAt,
                               String lastUpdatedAt) {
}
