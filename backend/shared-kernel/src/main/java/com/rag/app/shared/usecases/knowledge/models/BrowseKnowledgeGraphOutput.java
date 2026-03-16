package com.rag.app.shared.usecases.knowledge.models;

import com.rag.app.shared.domain.knowledge.entities.KnowledgeGraph;
import com.rag.app.shared.domain.knowledge.entities.KnowledgeNode;
import com.rag.app.shared.domain.knowledge.entities.KnowledgeRelationship;

import java.util.List;
import java.util.Objects;

public record BrowseKnowledgeGraphOutput(List<KnowledgeGraph> graphs,
                                         List<KnowledgeNode> nodes,
                                         List<KnowledgeRelationship> relationships,
                                         List<KnowledgeNode> connectedNodes,
                                         int totalResults,
                                         String errorMessage) {
    public BrowseKnowledgeGraphOutput {
        graphs = List.copyOf(Objects.requireNonNull(graphs, "graphs cannot be null"));
        nodes = List.copyOf(Objects.requireNonNull(nodes, "nodes cannot be null"));
        relationships = List.copyOf(Objects.requireNonNull(relationships, "relationships cannot be null"));
        connectedNodes = List.copyOf(Objects.requireNonNull(connectedNodes, "connectedNodes cannot be null"));
        if (totalResults < 0) {
            throw new IllegalArgumentException("totalResults cannot be negative");
        }
    }

    public static BrowseKnowledgeGraphOutput success(List<KnowledgeGraph> graphs,
                                                     List<KnowledgeNode> nodes,
                                                     List<KnowledgeRelationship> relationships,
                                                     List<KnowledgeNode> connectedNodes,
                                                     int totalResults) {
        return new BrowseKnowledgeGraphOutput(graphs, nodes, relationships, connectedNodes, totalResults, null);
    }

    public static BrowseKnowledgeGraphOutput notFound(String errorMessage) {
        return new BrowseKnowledgeGraphOutput(List.of(), List.of(), List.of(), List.of(), 0, errorMessage);
    }

    public boolean found() {
        return errorMessage == null;
    }
}
