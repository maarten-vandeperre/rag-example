package com.rag.app.shared.usecases.knowledge.models;

import com.rag.app.shared.domain.knowledge.valueobjects.GraphId;

public record BuildKnowledgeGraphOutput(GraphId graphId,
                                        int totalNodes,
                                        int totalRelationships,
                                        boolean wasExtended,
                                        boolean success,
                                        String errorMessage) {
    public static BuildKnowledgeGraphOutput success(GraphId graphId,
                                                    int totalNodes,
                                                    int totalRelationships,
                                                    boolean wasExtended) {
        return new BuildKnowledgeGraphOutput(graphId, totalNodes, totalRelationships, wasExtended, true, null);
    }

    public static BuildKnowledgeGraphOutput failure(String errorMessage) {
        return new BuildKnowledgeGraphOutput(null, 0, 0, false, false, errorMessage);
    }
}
