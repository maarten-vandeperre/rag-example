package com.rag.app.shared.usecases.knowledge;

import com.rag.app.shared.domain.knowledge.entities.KnowledgeGraph;
import com.rag.app.shared.domain.knowledge.entities.KnowledgeNode;
import com.rag.app.shared.domain.knowledge.entities.KnowledgeRelationship;
import com.rag.app.shared.interfaces.UseCase;
import com.rag.app.shared.interfaces.knowledge.KnowledgeGraphRepository;
import com.rag.app.shared.usecases.knowledge.models.SearchKnowledgeGraphInput;
import com.rag.app.shared.usecases.knowledge.models.SearchKnowledgeGraphOutput;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class SearchKnowledgeGraph implements UseCase<SearchKnowledgeGraphInput, SearchKnowledgeGraphOutput> {
    private final KnowledgeGraphRepository knowledgeGraphRepository;

    public SearchKnowledgeGraph(KnowledgeGraphRepository knowledgeGraphRepository) {
        this.knowledgeGraphRepository = Objects.requireNonNull(knowledgeGraphRepository, "knowledgeGraphRepository cannot be null");
    }

    @Override
    public SearchKnowledgeGraphOutput execute(SearchKnowledgeGraphInput input) {
        Objects.requireNonNull(input, "input cannot be null");

        List<KnowledgeGraph> graphs = input.graphId() == null
            ? knowledgeGraphRepository.findAll()
            : knowledgeGraphRepository.findById(input.graphId()).stream().toList();
        String normalizedQuery = input.query().toLowerCase(Locale.ROOT);

        List<KnowledgeNode> matchedNodes = graphs.stream()
            .flatMap(graph -> graph.nodes().stream())
            .filter(node -> input.nodeTypes().isEmpty() || input.nodeTypes().contains(node.nodeType()))
            .filter(node -> matches(normalizedQuery, node.label()) || node.properties().values().stream().anyMatch(value -> matches(normalizedQuery, String.valueOf(value))))
            .sorted(Comparator.comparing(node -> node.label().toLowerCase(Locale.ROOT)))
            .toList();

        List<KnowledgeRelationship> matchedRelationships = graphs.stream()
            .flatMap(graph -> graph.relationships().stream()
                .filter(relationship -> input.relationshipTypes().isEmpty() || input.relationshipTypes().contains(relationship.relationshipType()))
                .filter(relationship -> matches(normalizedQuery, relationship.relationshipType().name())
                    || relationship.properties().values().stream().anyMatch(value -> matches(normalizedQuery, String.valueOf(value)))
                    || graph.getNode(relationship.fromNodeId()).map(node -> matches(normalizedQuery, node.label())).orElse(false)
                    || graph.getNode(relationship.toNodeId()).map(node -> matches(normalizedQuery, node.label())).orElse(false)))
            .sorted(Comparator.comparing(relationship -> relationship.relationshipType().name()))
            .toList();

        LinkedHashSet<KnowledgeGraph> matchedGraphs = new LinkedHashSet<>();
        for (KnowledgeGraph graph : graphs) {
            boolean hasNodeMatch = graph.nodes().stream().anyMatch(matchedNodes::contains);
            boolean hasRelationshipMatch = graph.relationships().stream().anyMatch(matchedRelationships::contains);
            if (hasNodeMatch || hasRelationshipMatch || matches(normalizedQuery, graph.name()) || matches(normalizedQuery, graph.metadata().description())) {
                matchedGraphs.add(graph);
            }
        }

        int totalResults = matchedNodes.size() + matchedRelationships.size();
        return new SearchKnowledgeGraphOutput(
            input.query(),
            List.copyOf(matchedGraphs),
            paginate(matchedNodes, input.page(), input.size()),
            paginate(matchedRelationships, input.page(), input.size()),
            totalResults
        );
    }

    private boolean matches(String normalizedQuery, String candidate) {
        return candidate != null && candidate.toLowerCase(Locale.ROOT).contains(normalizedQuery);
    }

    private <T> List<T> paginate(List<T> values, int page, int size) {
        int start = Math.min(page * size, values.size());
        int end = Math.min(start + size, values.size());
        return new ArrayList<>(values.subList(start, end));
    }
}
