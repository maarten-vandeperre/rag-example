package com.rag.app.shared.usecases.knowledge.models;

import java.util.Map;
import java.util.Objects;

public record GetKnowledgeGraphStatisticsOutput(int totalGraphs,
                                                int totalNodes,
                                                int totalRelationships,
                                                Map<String, Integer> nodeTypeCounts,
                                                Map<String, Integer> relationshipTypeCounts,
                                                double averageNodeConfidence,
                                                double averageRelationshipConfidence) {
    public GetKnowledgeGraphStatisticsOutput {
        nodeTypeCounts = Map.copyOf(Objects.requireNonNull(nodeTypeCounts, "nodeTypeCounts cannot be null"));
        relationshipTypeCounts = Map.copyOf(Objects.requireNonNull(relationshipTypeCounts, "relationshipTypeCounts cannot be null"));
    }
}
