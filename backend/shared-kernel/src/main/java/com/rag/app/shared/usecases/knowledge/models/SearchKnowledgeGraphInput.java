package com.rag.app.shared.usecases.knowledge.models;

import com.rag.app.shared.domain.exceptions.ValidationException;
import com.rag.app.shared.domain.knowledge.valueobjects.GraphId;
import com.rag.app.shared.domain.knowledge.valueobjects.NodeType;
import com.rag.app.shared.domain.knowledge.valueobjects.RelationshipType;

import java.util.List;
import java.util.Objects;

public record SearchKnowledgeGraphInput(String query,
                                        GraphId graphId,
                                        List<NodeType> nodeTypes,
                                        List<RelationshipType> relationshipTypes,
                                        int page,
                                        int size) {
    public SearchKnowledgeGraphInput {
        if (query == null || query.isBlank()) {
            throw new ValidationException("query cannot be null or blank");
        }
        if (page < 0) {
            throw new ValidationException("page cannot be negative");
        }
        if (size <= 0) {
            throw new ValidationException("size must be positive");
        }
        query = query.trim();
        nodeTypes = nodeTypes == null ? List.of() : List.copyOf(nodeTypes);
        relationshipTypes = relationshipTypes == null ? List.of() : List.copyOf(relationshipTypes);
    }
}
