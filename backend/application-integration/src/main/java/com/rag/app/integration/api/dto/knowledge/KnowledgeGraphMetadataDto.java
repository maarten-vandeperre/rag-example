package com.rag.app.integration.api.dto.knowledge;

import java.util.Map;

public record KnowledgeGraphMetadataDto(String description,
                                        int sourceDocumentCount,
                                        Map<String, Object> attributes) {
}
