package com.rag.app.shared.usecases.knowledge;

import com.rag.app.shared.domain.knowledge.entities.KnowledgeGraph;
import com.rag.app.shared.interfaces.UseCase;
import com.rag.app.shared.interfaces.knowledge.KnowledgeGraphRepository;
import com.rag.app.shared.usecases.knowledge.models.GetKnowledgeGraphStatisticsInput;
import com.rag.app.shared.usecases.knowledge.models.GetKnowledgeGraphStatisticsOutput;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class GetKnowledgeGraphStatistics implements UseCase<GetKnowledgeGraphStatisticsInput, GetKnowledgeGraphStatisticsOutput> {
    private final KnowledgeGraphRepository knowledgeGraphRepository;

    public GetKnowledgeGraphStatistics(KnowledgeGraphRepository knowledgeGraphRepository) {
        this.knowledgeGraphRepository = Objects.requireNonNull(knowledgeGraphRepository, "knowledgeGraphRepository cannot be null");
    }

    @Override
    public GetKnowledgeGraphStatisticsOutput execute(GetKnowledgeGraphStatisticsInput input) {
        Objects.requireNonNull(input, "input cannot be null");
        List<KnowledgeGraph> graphs = input.graphId() == null
            ? knowledgeGraphRepository.findAll()
            : knowledgeGraphRepository.findById(input.graphId()).stream().toList();

        int totalNodes = graphs.stream().mapToInt(graph -> graph.nodes().size()).sum();
        int totalRelationships = graphs.stream().mapToInt(graph -> graph.relationships().size()).sum();
        Map<String, Integer> nodeTypeCounts = new LinkedHashMap<>();
        Map<String, Integer> relationshipTypeCounts = new LinkedHashMap<>();

        double totalNodeConfidence = 0.0d;
        double totalRelationshipConfidence = 0.0d;
        int nodeCount = 0;
        int relationshipCount = 0;

        for (KnowledgeGraph graph : graphs) {
            for (var node : graph.nodes()) {
                nodeTypeCounts.merge(node.nodeType().name(), 1, Integer::sum);
                totalNodeConfidence += node.confidence().value();
                nodeCount++;
            }
            for (var relationship : graph.relationships()) {
                relationshipTypeCounts.merge(relationship.relationshipType().name(), 1, Integer::sum);
                totalRelationshipConfidence += relationship.confidence().value();
                relationshipCount++;
            }
        }

        return new GetKnowledgeGraphStatisticsOutput(
            graphs.size(),
            totalNodes,
            totalRelationships,
            nodeTypeCounts,
            relationshipTypeCounts,
            nodeCount == 0 ? 0.0d : totalNodeConfidence / nodeCount,
            relationshipCount == 0 ? 0.0d : totalRelationshipConfidence / relationshipCount
        );
    }
}
