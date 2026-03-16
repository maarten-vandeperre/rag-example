package com.rag.app.shared.usecases.knowledge.models;

import com.rag.app.shared.domain.exceptions.ValidationException;
import com.rag.app.shared.domain.knowledge.valueobjects.GraphId;
import com.rag.app.shared.domain.knowledge.valueobjects.NodeId;

import java.util.Objects;

public record BrowseKnowledgeGraphInput(BrowseType browseType,
                                        GraphId graphId,
                                        NodeId nodeId,
                                        int page,
                                        int size,
                                        int depth) {
    public BrowseKnowledgeGraphInput(BrowseType browseType,
                                     GraphId graphId,
                                     NodeId nodeId,
                                     int page,
                                     int size) {
        this(browseType, graphId, nodeId, page, size, 1);
    }

    public BrowseKnowledgeGraphInput {
        Objects.requireNonNull(browseType, "browseType cannot be null");
        if (page < 0) {
            throw new ValidationException("page cannot be negative");
        }
        if (size <= 0) {
            throw new ValidationException("size must be positive");
        }
        if (depth < 1) {
            throw new ValidationException("depth must be at least 1");
        }
        if ((browseType == BrowseType.GET_GRAPH || browseType == BrowseType.GET_NODE_DETAILS || browseType == BrowseType.GET_SUBGRAPH) && graphId == null) {
            throw new ValidationException("graphId is required for " + browseType);
        }
        if ((browseType == BrowseType.GET_NODE_DETAILS || browseType == BrowseType.GET_SUBGRAPH) && nodeId == null) {
            throw new ValidationException("nodeId is required for " + browseType);
        }
    }

    public BrowseKnowledgeGraphInput withDepth(int updatedDepth) {
        return new BrowseKnowledgeGraphInput(browseType, graphId, nodeId, page, size, updatedDepth);
    }
}
