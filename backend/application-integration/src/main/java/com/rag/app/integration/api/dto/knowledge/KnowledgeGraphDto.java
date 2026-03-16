package com.rag.app.integration.api.dto.knowledge;

import java.util.List;

public record KnowledgeGraphDto(String graphId,
                                String name,
                                List<KnowledgeNodeDto> nodes,
                                List<KnowledgeRelationshipDto> relationships,
                                KnowledgeGraphMetadataDto metadata,
                                String createdAt,
                                String lastUpdatedAt,
                                int totalNodes,
                                int totalRelationships) {
}
