package com.rag.app.shared.usecases.knowledge;

import com.rag.app.shared.interfaces.UseCase;
import com.rag.app.shared.interfaces.knowledge.KnowledgeGraphRepository;
import com.rag.app.shared.usecases.knowledge.models.BrowseKnowledgeGraphInput;
import com.rag.app.shared.usecases.knowledge.models.BrowseKnowledgeGraphOutput;
import com.rag.app.shared.usecases.knowledge.models.BrowseType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class BrowseKnowledgeGraph implements UseCase<BrowseKnowledgeGraphInput, BrowseKnowledgeGraphOutput> {
    private final KnowledgeGraphRepository knowledgeGraphRepository;

    public BrowseKnowledgeGraph(KnowledgeGraphRepository knowledgeGraphRepository) {
        this.knowledgeGraphRepository = Objects.requireNonNull(knowledgeGraphRepository, "knowledgeGraphRepository cannot be null");
    }

    @Override
    public BrowseKnowledgeGraphOutput execute(BrowseKnowledgeGraphInput input) {
        Objects.requireNonNull(input, "input cannot be null");
        return switch (input.browseType()) {
            case LIST_GRAPHS -> listGraphs(input);
            case GET_GRAPH -> getGraph(input);
            case GET_NODE_DETAILS -> getNodeDetails(input);
            case GET_SUBGRAPH -> getSubgraph(input);
        };
    }

    private BrowseKnowledgeGraphOutput listGraphs(BrowseKnowledgeGraphInput input) {
        List<com.rag.app.shared.domain.knowledge.entities.KnowledgeGraph> graphs = knowledgeGraphRepository.findAll().stream()
            .sorted(Comparator.comparing(com.rag.app.shared.domain.knowledge.entities.KnowledgeGraph::name))
            .toList();
        return BrowseKnowledgeGraphOutput.success(paginate(graphs, input.page(), input.size()), List.of(), List.of(), List.of(), graphs.size());
    }

    private BrowseKnowledgeGraphOutput getGraph(BrowseKnowledgeGraphInput input) {
        return knowledgeGraphRepository.findById(input.graphId())
            .map(graph -> BrowseKnowledgeGraphOutput.success(
                List.of(graph),
                paginate(graph.nodes().stream().sorted(Comparator.comparing(node -> node.label().toLowerCase())).toList(), input.page(), input.size()),
                paginate(graph.relationships().stream().sorted(Comparator.comparing(relationship -> relationship.relationshipType().name())).toList(), input.page(), input.size()),
                List.of(),
                1
            ))
            .orElseGet(() -> BrowseKnowledgeGraphOutput.notFound("Graph not found: " + input.graphId()));
    }

    private BrowseKnowledgeGraphOutput getNodeDetails(BrowseKnowledgeGraphInput input) {
        return knowledgeGraphRepository.findById(input.graphId())
            .map(graph -> graph.getNode(input.nodeId())
                .map(node -> {
                    List<com.rag.app.shared.domain.knowledge.entities.KnowledgeNode> connectedNodes = knowledgeGraphRepository.findNodesConnectedTo(input.nodeId()).stream()
                        .filter(connectedNode -> graph.containsNode(connectedNode.nodeId()))
                        .toList();
                    List<com.rag.app.shared.domain.knowledge.entities.KnowledgeRelationship> relationships = graph.relationshipsFor(input.nodeId()).stream().toList();
                    return BrowseKnowledgeGraphOutput.success(List.of(graph), List.of(node), relationships, connectedNodes, 1);
                })
                .orElseGet(() -> BrowseKnowledgeGraphOutput.notFound("Node not found in graph: " + input.nodeId())))
            .orElseGet(() -> BrowseKnowledgeGraphOutput.notFound("Graph not found: " + input.graphId()));
    }

    private BrowseKnowledgeGraphOutput getSubgraph(BrowseKnowledgeGraphInput input) {
        com.rag.app.shared.domain.knowledge.entities.KnowledgeGraph subgraph = knowledgeGraphRepository.findSubgraphAroundNode(input.nodeId(), input.depth());
        return BrowseKnowledgeGraphOutput.success(
            List.of(subgraph),
            subgraph.nodes().stream().toList(),
            subgraph.relationships().stream().toList(),
            List.of(),
            1
        );
    }

    private <T> List<T> paginate(List<T> values, int page, int size) {
        int start = Math.min(page * size, values.size());
        int end = Math.min(start + size, values.size());
        return new ArrayList<>(values.subList(start, end));
    }
}
