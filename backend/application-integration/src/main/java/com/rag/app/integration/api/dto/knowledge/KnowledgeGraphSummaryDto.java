package com.rag.app.integration.api.dto.knowledge;

public record KnowledgeGraphSummaryDto(String graphId,
                                       String name,
                                       int totalNodes,
                                       int totalRelationships,
                                       String createdAt,
                                       String lastUpdatedAt) {
}
