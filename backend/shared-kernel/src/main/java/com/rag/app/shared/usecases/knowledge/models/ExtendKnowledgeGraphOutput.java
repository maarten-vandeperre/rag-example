package com.rag.app.shared.usecases.knowledge.models;

import com.rag.app.shared.domain.knowledge.valueobjects.GraphId;

public record ExtendKnowledgeGraphOutput(GraphId graphId,
                                         int totalNodes,
                                         int totalRelationships,
                                         boolean success,
                                         String errorMessage) {
    public static ExtendKnowledgeGraphOutput success(GraphId graphId, int totalNodes, int totalRelationships) {
        return new ExtendKnowledgeGraphOutput(graphId, totalNodes, totalRelationships, true, null);
    }

    public static ExtendKnowledgeGraphOutput failure(String errorMessage) {
        return new ExtendKnowledgeGraphOutput(null, 0, 0, false, errorMessage);
    }
}
