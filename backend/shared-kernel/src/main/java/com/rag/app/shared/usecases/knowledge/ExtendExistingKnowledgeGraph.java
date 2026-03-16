package com.rag.app.shared.usecases.knowledge;

import com.rag.app.shared.domain.knowledge.entities.KnowledgeGraph;
import com.rag.app.shared.domain.knowledge.services.KnowledgeGraphDomainService;
import com.rag.app.shared.interfaces.UseCase;
import com.rag.app.shared.interfaces.knowledge.KnowledgeGraphRepository;
import com.rag.app.shared.usecases.knowledge.models.ExtendKnowledgeGraphInput;
import com.rag.app.shared.usecases.knowledge.models.ExtendKnowledgeGraphOutput;

import java.util.Objects;

public final class ExtendExistingKnowledgeGraph implements UseCase<ExtendKnowledgeGraphInput, ExtendKnowledgeGraphOutput> {
    private final KnowledgeGraphRepository knowledgeGraphRepository;
    private final KnowledgeGraphDomainService knowledgeGraphDomainService;

    public ExtendExistingKnowledgeGraph(KnowledgeGraphRepository knowledgeGraphRepository,
                                        KnowledgeGraphDomainService knowledgeGraphDomainService) {
        this.knowledgeGraphRepository = Objects.requireNonNull(knowledgeGraphRepository, "knowledgeGraphRepository cannot be null");
        this.knowledgeGraphDomainService = Objects.requireNonNull(knowledgeGraphDomainService, "knowledgeGraphDomainService cannot be null");
    }

    @Override
    public ExtendKnowledgeGraphOutput execute(ExtendKnowledgeGraphInput input) {
        Objects.requireNonNull(input, "input cannot be null");

        return knowledgeGraphRepository.findById(input.graphId())
            .map(existingGraph -> extend(existingGraph, input))
            .orElseGet(() -> ExtendKnowledgeGraphOutput.failure("Knowledge graph not found for graphId: " + input.graphId()));
    }

    private ExtendKnowledgeGraphOutput extend(KnowledgeGraph existingGraph, ExtendKnowledgeGraphInput input) {
        try {
            KnowledgeGraph mergedGraph = knowledgeGraphDomainService.mergeExtractedKnowledge(existingGraph, input.extractedKnowledge());
            knowledgeGraphDomainService.validateGraphConsistency(mergedGraph);
            KnowledgeGraph savedGraph = knowledgeGraphRepository.save(mergedGraph);
            return ExtendKnowledgeGraphOutput.success(savedGraph.graphId(), savedGraph.nodes().size(), savedGraph.relationships().size());
        } catch (RuntimeException exception) {
            return ExtendKnowledgeGraphOutput.failure("Failed to extend knowledge graph: " + exception.getMessage());
        }
    }
}
