package com.rag.app.integration.api.dto.knowledge;

import com.rag.app.shared.domain.knowledge.entities.KnowledgeGraph;
import com.rag.app.shared.domain.knowledge.entities.KnowledgeNode;
import com.rag.app.shared.domain.knowledge.entities.KnowledgeRelationship;
import com.rag.app.shared.usecases.knowledge.models.GetKnowledgeGraphStatisticsOutput;
import com.rag.app.shared.usecases.knowledge.models.SearchKnowledgeGraphOutput;

import java.util.List;

public final class KnowledgeGraphDtoMapper {
    public KnowledgeGraphSummaryDto toSummaryDto(KnowledgeGraph graph) {
        return new KnowledgeGraphSummaryDto(
            graph.graphId().value(),
            graph.name(),
            graph.nodes().size(),
            graph.relationships().size(),
            graph.createdAt().toString(),
            graph.lastUpdatedAt().toString()
        );
    }

    public KnowledgeGraphDto toDto(KnowledgeGraph graph, List<KnowledgeNode> nodes, List<KnowledgeRelationship> relationships) {
        return new KnowledgeGraphDto(
            graph.graphId().value(),
            graph.name(),
            nodes.stream().map(this::toNodeDto).toList(),
            relationships.stream().map(this::toRelationshipDto).toList(),
            new KnowledgeGraphMetadataDto(graph.metadata().description(), graph.metadata().sourceDocuments().size(), graph.metadata().attributes()),
            graph.createdAt().toString(),
            graph.lastUpdatedAt().toString(),
            graph.nodes().size(),
            graph.relationships().size()
        );
    }

    public KnowledgeNodeDto toNodeDto(KnowledgeNode node) {
        return new KnowledgeNodeDto(
            node.nodeId().value(),
            node.label(),
            node.nodeType().name(),
            node.properties(),
            KnowledgeDocumentReferenceDto.fromDomain(node.sourceDocument()),
            node.confidence().value(),
            node.createdAt().toString(),
            node.lastUpdatedAt().toString()
        );
    }

    public KnowledgeRelationshipDto toRelationshipDto(KnowledgeRelationship relationship) {
        return new KnowledgeRelationshipDto(
            relationship.relationshipId().value(),
            relationship.fromNodeId().value(),
            relationship.toNodeId().value(),
            relationship.relationshipType().name(),
            relationship.properties(),
            KnowledgeDocumentReferenceDto.fromDomain(relationship.sourceDocument()),
            relationship.confidence().value(),
            relationship.createdAt().toString()
        );
    }

    public KnowledgeNodeDetailDto toNodeDetailDto(KnowledgeNode node, List<KnowledgeNode> connectedNodes, List<KnowledgeRelationship> relationships) {
        return new KnowledgeNodeDetailDto(
            toNodeDto(node),
            connectedNodes.stream().map(this::toNodeDto).toList(),
            relationships.stream().map(this::toRelationshipDto).toList(),
            connectedNodes.size(),
            relationships.stream().map(relationship -> relationship.relationshipType().name()).distinct().toList()
        );
    }

    public KnowledgeSubgraphDto toSubgraphDto(List<KnowledgeNode> nodes, List<KnowledgeRelationship> relationships, String centerNodeId, int depth) {
        List<KnowledgeNodeDto> nodeDtos = nodes.stream().map(this::toNodeDto).toList();
        List<KnowledgeRelationshipDto> relationshipDtos = relationships.stream().map(this::toRelationshipDto).toList();
        return new KnowledgeSubgraphDto(centerNodeId, depth, nodeDtos, relationshipDtos, GraphVisualizationDto.from(nodeDtos, relationshipDtos));
    }

    public KnowledgeSearchResultDto toSearchResultDto(SearchKnowledgeGraphOutput output) {
        return new KnowledgeSearchResultDto(
            output.query(),
            output.nodes().stream().map(this::toNodeDto).toList(),
            output.relationships().stream().map(this::toRelationshipDto).toList(),
            output.graphs().stream().map(this::toSummaryDto).toList(),
            output.totalResults()
        );
    }

    public KnowledgeGraphStatisticsDto toStatisticsDto(GetKnowledgeGraphStatisticsOutput output) {
        return new KnowledgeGraphStatisticsDto(
            output.totalGraphs(),
            output.totalNodes(),
            output.totalRelationships(),
            output.nodeTypeCounts(),
            output.relationshipTypeCounts(),
            output.averageNodeConfidence(),
            output.averageRelationshipConfidence()
        );
    }
}
