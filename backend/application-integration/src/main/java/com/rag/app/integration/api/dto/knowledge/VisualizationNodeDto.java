package com.rag.app.integration.api.dto.knowledge;

public record VisualizationNodeDto(String id,
                                   String label,
                                   String type,
                                   double confidence,
                                   String color,
                                   int size) {
}
