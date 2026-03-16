package com.rag.app.integration.api.dto.knowledge;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record GraphVisualizationDto(List<VisualizationNodeDto> nodes,
                                    List<VisualizationEdgeDto> edges,
                                    Map<String, Integer> nodeTypeCounts,
                                    Map<String, Integer> relationshipTypeCounts) {
    public static GraphVisualizationDto from(List<KnowledgeNodeDto> nodes, List<KnowledgeRelationshipDto> relationships) {
        List<VisualizationNodeDto> visualizationNodes = nodes.stream()
            .map(node -> new VisualizationNodeDto(node.nodeId(), node.label(), node.nodeType(), node.confidence(), nodeColor(node.nodeType()), nodeSize(node.confidence())))
            .toList();
        List<VisualizationEdgeDto> visualizationEdges = relationships.stream()
            .map(relationship -> new VisualizationEdgeDto(
                relationship.relationshipId(),
                relationship.fromNodeId(),
                relationship.toNodeId(),
                relationship.relationshipType(),
                relationship.confidence(),
                edgeColor(relationship.relationshipType()),
                edgeWidth(relationship.confidence())
            ))
            .toList();
        return new GraphVisualizationDto(
            visualizationNodes,
            visualizationEdges,
            nodes.stream().collect(Collectors.groupingBy(KnowledgeNodeDto::nodeType, Collectors.summingInt(node -> 1))),
            relationships.stream().collect(Collectors.groupingBy(KnowledgeRelationshipDto::relationshipType, Collectors.summingInt(relationship -> 1)))
        );
    }

    private static String nodeColor(String nodeType) {
        return switch (nodeType) {
            case "CONCEPT" -> "#4CAF50";
            case "ENTITY" -> "#2196F3";
            case "PERSON" -> "#FF9800";
            case "ORGANIZATION" -> "#9C27B0";
            case "LOCATION" -> "#F44336";
            default -> "#757575";
        };
    }

    private static int nodeSize(double confidence) {
        if (confidence >= 0.8d) {
            return 20;
        }
        if (confidence >= 0.6d) {
            return 15;
        }
        return 10;
    }

    private static String edgeColor(String relationshipType) {
        return switch (relationshipType) {
            case "RELATED_TO" -> "#666666";
            case "PART_OF" -> "#4CAF50";
            case "MENTIONS" -> "#2196F3";
            case "REFERENCES" -> "#FF9800";
            default -> "#CCCCCC";
        };
    }

    private static int edgeWidth(double confidence) {
        if (confidence >= 0.8d) {
            return 3;
        }
        if (confidence >= 0.6d) {
            return 2;
        }
        return 1;
    }
}
