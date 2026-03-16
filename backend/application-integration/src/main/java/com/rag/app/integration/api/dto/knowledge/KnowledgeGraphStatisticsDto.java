package com.rag.app.integration.api.dto.knowledge;

import java.util.Map;

public record KnowledgeGraphStatisticsDto(int totalGraphs,
                                          int totalNodes,
                                          int totalRelationships,
                                          Map<String, Integer> nodeTypeCounts,
                                          Map<String, Integer> relationshipTypeCounts,
                                          double averageNodeConfidence,
                                          double averageRelationshipConfidence) {
}
