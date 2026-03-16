package com.rag.app.shared.usecases.knowledge.models;

import com.rag.app.shared.domain.knowledge.valueobjects.ExtractedKnowledge;
import com.rag.app.shared.domain.knowledge.valueobjects.GraphId;

import java.util.Objects;

public record ExtendKnowledgeGraphInput(GraphId graphId,
                                        ExtractedKnowledge extractedKnowledge) {
    public ExtendKnowledgeGraphInput {
        Objects.requireNonNull(graphId, "graphId cannot be null");
        Objects.requireNonNull(extractedKnowledge, "extractedKnowledge cannot be null");
    }
}
