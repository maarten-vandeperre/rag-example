package com.rag.app.integration.api.dto.knowledge;

public record VisualizationEdgeDto(String id,
                                   String fromNodeId,
                                   String toNodeId,
                                   String relationshipType,
                                   double confidence,
                                   String color,
                                   int width) {
}
