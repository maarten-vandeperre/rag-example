package com.rag.app.shared.usecases.knowledge;

import com.rag.app.shared.domain.knowledge.entities.KnowledgeGraph;
import com.rag.app.shared.domain.knowledge.services.KnowledgeGraphDomainService;
import com.rag.app.shared.domain.knowledge.valueobjects.GraphId;
import com.rag.app.shared.domain.knowledge.valueobjects.GraphMetadata;
import com.rag.app.shared.interfaces.UseCase;
import com.rag.app.shared.interfaces.knowledge.KnowledgeGraphRepository;
import com.rag.app.shared.usecases.knowledge.models.BuildKnowledgeGraphInput;
import com.rag.app.shared.usecases.knowledge.models.BuildKnowledgeGraphOutput;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class BuildKnowledgeGraph implements UseCase<BuildKnowledgeGraphInput, BuildKnowledgeGraphOutput> {
    private final KnowledgeGraphRepository knowledgeGraphRepository;
    private final KnowledgeGraphDomainService knowledgeGraphDomainService;
    private final Clock clock;

    public BuildKnowledgeGraph(KnowledgeGraphRepository knowledgeGraphRepository,
                               KnowledgeGraphDomainService knowledgeGraphDomainService,
                               Clock clock) {
        this.knowledgeGraphRepository = Objects.requireNonNull(knowledgeGraphRepository, "knowledgeGraphRepository cannot be null");
        this.knowledgeGraphDomainService = Objects.requireNonNull(knowledgeGraphDomainService, "knowledgeGraphDomainService cannot be null");
        this.clock = Objects.requireNonNull(clock, "clock cannot be null");
    }

    @Override
    public BuildKnowledgeGraphOutput execute(BuildKnowledgeGraphInput input) {
        Objects.requireNonNull(input, "input cannot be null");

        try {
            Optional<KnowledgeGraph> existingGraph = knowledgeGraphRepository.findByName(input.graphName());
            if (existingGraph.isPresent() && !input.allowMerging()) {
                return BuildKnowledgeGraphOutput.failure("Knowledge graph already exists and merging is disabled");
            }

            KnowledgeGraph resultGraph = existingGraph.isPresent()
                ? knowledgeGraphDomainService.mergeExtractedKnowledge(existingGraph.get(), input.extractedKnowledge())
                : createNewGraph(input.graphName(), input.extractedKnowledge());

            knowledgeGraphDomainService.validateGraphConsistency(resultGraph);
            KnowledgeGraph savedGraph = knowledgeGraphRepository.save(resultGraph);

            return BuildKnowledgeGraphOutput.success(
                savedGraph.graphId(),
                savedGraph.nodes().size(),
                savedGraph.relationships().size(),
                existingGraph.isPresent()
            );
        } catch (RuntimeException exception) {
            return BuildKnowledgeGraphOutput.failure("Failed to build knowledge graph: " + exception.getMessage());
        }
    }

    private KnowledgeGraph createNewGraph(String graphName, com.rag.app.shared.domain.knowledge.valueobjects.ExtractedKnowledge extractedKnowledge) {
        Instant now = clock.instant();
        return new KnowledgeGraph(
            GraphId.generate(),
            graphName,
            new LinkedHashSet<>(extractedKnowledge.nodes()),
            new LinkedHashSet<>(extractedKnowledge.relationships()),
            graphMetadata(extractedKnowledge),
            now,
            now
        );
    }

    private GraphMetadata graphMetadata(com.rag.app.shared.domain.knowledge.valueobjects.ExtractedKnowledge extractedKnowledge) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("extractionMethod", extractedKnowledge.metadata().extractionMethod());
        attributes.put("extractedAt", extractedKnowledge.metadata().extractedAt().toString());
        attributes.put("processingTimeMs", extractedKnowledge.metadata().processingTime().toMillis());
        attributes.put("warnings", extractedKnowledge.metadata().warnings());
        Set<com.rag.app.shared.domain.knowledge.valueobjects.DocumentReference> sourceDocuments = new LinkedHashSet<>();
        sourceDocuments.add(extractedKnowledge.sourceDocument());
        return new GraphMetadata("Knowledge graph built from extracted document knowledge", sourceDocuments, attributes);
    }
}
