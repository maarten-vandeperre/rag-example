package com.rag.app.shared.usecases.knowledge.models;

import com.rag.app.shared.domain.knowledge.entities.KnowledgeGraph;
import com.rag.app.shared.domain.knowledge.entities.KnowledgeNode;
import com.rag.app.shared.domain.knowledge.entities.KnowledgeRelationship;

import java.util.List;
import java.util.Objects;

public record SearchKnowledgeGraphOutput(String query,
                                         List<KnowledgeGraph> graphs,
                                         List<KnowledgeNode> nodes,
                                         List<KnowledgeRelationship> relationships,
                                         int totalResults) {
    public SearchKnowledgeGraphOutput {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("query cannot be null or blank");
        }
        graphs = List.copyOf(Objects.requireNonNull(graphs, "graphs cannot be null"));
        nodes = List.copyOf(Objects.requireNonNull(nodes, "nodes cannot be null"));
        relationships = List.copyOf(Objects.requireNonNull(relationships, "relationships cannot be null"));
        if (totalResults < 0) {
            throw new IllegalArgumentException("totalResults cannot be negative");
        }
    }
}
