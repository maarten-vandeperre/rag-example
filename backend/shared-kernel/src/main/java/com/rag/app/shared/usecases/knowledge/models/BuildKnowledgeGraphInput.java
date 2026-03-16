package com.rag.app.shared.usecases.knowledge.models;

import com.rag.app.shared.domain.exceptions.ValidationException;
import com.rag.app.shared.domain.knowledge.valueobjects.ExtractedKnowledge;

import java.util.Objects;

public record BuildKnowledgeGraphInput(String graphName,
                                       ExtractedKnowledge extractedKnowledge,
                                       boolean allowMerging) {
    public BuildKnowledgeGraphInput {
        if (graphName == null || graphName.isBlank()) {
            throw new ValidationException("graphName cannot be null or blank");
        }
        Objects.requireNonNull(extractedKnowledge, "extractedKnowledge cannot be null");
        graphName = graphName.trim();
    }
}
