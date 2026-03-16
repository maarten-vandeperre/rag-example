package com.rag.app.shared.usecases.knowledge;

import com.rag.app.shared.domain.knowledge.entities.KnowledgeGraph;
import com.rag.app.shared.domain.knowledge.entities.KnowledgeNode;
import com.rag.app.shared.domain.knowledge.entities.KnowledgeRelationship;
import com.rag.app.shared.domain.knowledge.services.KnowledgeGraphDomainService;
import com.rag.app.shared.domain.knowledge.valueobjects.ConfidenceScore;
import com.rag.app.shared.domain.knowledge.valueobjects.DocumentReference;
import com.rag.app.shared.domain.knowledge.valueobjects.ExtractedKnowledge;
import com.rag.app.shared.domain.knowledge.valueobjects.ExtractionMetadata;
import com.rag.app.shared.domain.knowledge.valueobjects.GraphId;
import com.rag.app.shared.domain.knowledge.valueobjects.GraphMetadata;
import com.rag.app.shared.domain.knowledge.valueobjects.NodeId;
import com.rag.app.shared.domain.knowledge.valueobjects.NodeType;
import com.rag.app.shared.domain.knowledge.valueobjects.RelationshipType;
import com.rag.app.shared.interfaces.knowledge.DocumentQualityResult;
import com.rag.app.shared.interfaces.knowledge.DocumentQualityValidator;
import com.rag.app.shared.interfaces.knowledge.KnowledgeExtractionException;
import com.rag.app.shared.interfaces.knowledge.KnowledgeExtractionService;
import com.rag.app.shared.interfaces.knowledge.KnowledgeGraphRepository;
import com.rag.app.shared.interfaces.knowledge.UnsupportedDocumentFormatException;
import com.rag.app.shared.usecases.knowledge.models.BuildKnowledgeGraphInput;
import com.rag.app.shared.usecases.knowledge.models.BuildKnowledgeGraphOutput;
import com.rag.app.shared.usecases.knowledge.models.ExtendKnowledgeGraphInput;
import com.rag.app.shared.usecases.knowledge.models.ExtendKnowledgeGraphOutput;
import com.rag.app.shared.usecases.knowledge.models.ExtractKnowledgeInput;
import com.rag.app.shared.usecases.knowledge.models.ExtractKnowledgeOutput;
import com.rag.app.shared.usecases.knowledge.models.KnowledgeExtractionStatus;
import com.rag.app.shared.usecases.knowledge.models.ValidateKnowledgeQualityInput;
import com.rag.app.shared.usecases.knowledge.models.ValidateKnowledgeQualityOutput;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnowledgeExtractionUseCasesTest {
    private static final Instant BASE_TIME = Instant.parse("2026-03-16T12:00:00Z");
    private static final DocumentReference SOURCE_DOCUMENT = new DocumentReference(
        UUID.fromString("00000000-0000-0000-0000-000000000068"),
        "upload.md",
        "intro",
        0.88d
    );

    @Test
    void shouldExtractKnowledgeSuccessfullyAndTrackWarnings() {
        StubDocumentQualityValidator validator = new StubDocumentQualityValidator(
            new DocumentQualityResult(true, List.of("minor formatting issues"), List.of())
        );
        StubKnowledgeExtractionService extractionService = new StubKnowledgeExtractionService(sampleExtractedKnowledge(List.of("low confidence relationship")));
        ExtractKnowledgeFromDocument useCase = new ExtractKnowledgeFromDocument(extractionService, validator, new TickingClock(BASE_TIME));

        ExtractKnowledgeOutput output = useCase.execute(new ExtractKnowledgeInput(
            SOURCE_DOCUMENT.documentId().toString(),
            "A".repeat(150),
            SOURCE_DOCUMENT.documentName(),
            "markdown",
            Map.of("entities_enabled", true)
        ));

        assertTrue(output.isSuccessful());
        assertEquals(KnowledgeExtractionStatus.PARTIAL_SUCCESS, output.status());
        assertEquals(2, output.warnings().size());
        assertTrue(output.processingTime().toMillis() >= 1000);
        assertEquals(Map.of("defaultOption", true, "entities_enabled", true), extractionService.lastOptions);
    }

    @Test
    void shouldReturnInsufficientContentWhenQualityValidationFails() {
        StubDocumentQualityValidator validator = new StubDocumentQualityValidator(
            new DocumentQualityResult(false, List.of(), List.of("Document too short for extraction"))
        );
        ExtractKnowledgeFromDocument useCase = new ExtractKnowledgeFromDocument(
            new StubKnowledgeExtractionService(sampleExtractedKnowledge(List.of())),
            validator,
            Clock.fixed(BASE_TIME, ZoneOffset.UTC)
        );

        ExtractKnowledgeOutput output = useCase.execute(new ExtractKnowledgeInput(
            "doc-short",
            "A".repeat(120),
            "short.md",
            "markdown",
            Map.of()
        ));

        assertFalse(output.isSuccessful());
        assertEquals(KnowledgeExtractionStatus.INSUFFICIENT_CONTENT, output.status());
        assertTrue(output.warnings().contains("Document too short for extraction"));
        assertTrue(output.extractedKnowledge().isEmpty());
    }

    @Test
    void shouldReturnUnsupportedFormatAndProcessingErrors() {
        StubDocumentQualityValidator validator = new StubDocumentQualityValidator(DocumentQualityResult.sufficient(List.of()));
        StubKnowledgeExtractionService unsupportedService = new StubKnowledgeExtractionService(sampleExtractedKnowledge(List.of()));
        unsupportedService.supported = false;
        ExtractKnowledgeFromDocument unsupportedUseCase = new ExtractKnowledgeFromDocument(unsupportedService, validator, Clock.fixed(BASE_TIME, ZoneOffset.UTC));

        ExtractKnowledgeOutput unsupportedOutput = unsupportedUseCase.execute(new ExtractKnowledgeInput(
            "doc-unsupported",
            "A".repeat(140),
            "file.pdf",
            "pdf",
            Map.of()
        ));

        StubKnowledgeExtractionService failingService = new StubKnowledgeExtractionService(sampleExtractedKnowledge(List.of()));
        failingService.exception = new UnsupportedDocumentFormatException("OCR-only PDFs are not supported");
        ExtractKnowledgeFromDocument failingUseCase = new ExtractKnowledgeFromDocument(failingService, validator, Clock.fixed(BASE_TIME, ZoneOffset.UTC));

        ExtractKnowledgeOutput processingOutput = failingUseCase.execute(new ExtractKnowledgeInput(
            "doc-error",
            "A".repeat(140),
            "ocr.pdf",
            "markdown",
            Map.of()
        ));

        assertEquals(KnowledgeExtractionStatus.UNSUPPORTED_FORMAT, unsupportedOutput.status());
        assertTrue(unsupportedOutput.hasErrors());
        assertEquals(KnowledgeExtractionStatus.UNSUPPORTED_FORMAT, processingOutput.status());
        assertTrue(processingOutput.errors().contains("OCR-only PDFs are not supported"));
    }

    @Test
    void shouldValidateKnowledgeQualityThroughDedicatedUseCase() {
        ValidateKnowledgeQuality useCase = new ValidateKnowledgeQuality(
            new StubDocumentQualityValidator(new DocumentQualityResult(true, List.of("contains scanned tables"), List.of()))
        );

        ValidateKnowledgeQualityOutput output = useCase.execute(new ValidateKnowledgeQualityInput("A".repeat(140), "markdown"));

        assertTrue(output.sufficientForExtraction());
        assertTrue(output.hasWarnings());
        assertEquals(List.of("contains scanned tables"), output.warnings());
    }

    @Test
    void shouldBuildAndExtendKnowledgeGraphs() {
        InMemoryKnowledgeGraphRepository repository = new InMemoryKnowledgeGraphRepository();
        KnowledgeGraphDomainService domainService = new KnowledgeGraphDomainService();
        BuildKnowledgeGraph buildKnowledgeGraph = new BuildKnowledgeGraph(repository, domainService, Clock.fixed(BASE_TIME, ZoneOffset.UTC));

        BuildKnowledgeGraphOutput buildOutput = buildKnowledgeGraph.execute(new BuildKnowledgeGraphInput("project-knowledge", sampleExtractedKnowledge(List.of()), false));
        GraphId graphId = buildOutput.graphId();

        BuildKnowledgeGraphOutput duplicateOutput = buildKnowledgeGraph.execute(new BuildKnowledgeGraphInput("project-knowledge", sampleExtractedKnowledge(List.of()), false));

        ExtendExistingKnowledgeGraph extendKnowledgeGraph = new ExtendExistingKnowledgeGraph(repository, domainService);
        ExtendKnowledgeGraphOutput extendOutput = extendKnowledgeGraph.execute(new ExtendKnowledgeGraphInput(graphId, additionalKnowledge()));

        KnowledgeGraph storedGraph = repository.findById(graphId).orElseThrow();
        assertTrue(buildOutput.success());
        assertFalse(duplicateOutput.success());
        assertTrue(extendOutput.success());
        assertEquals(3, storedGraph.nodes().size());
        assertEquals(2, storedGraph.relationships().size());
    }

    @Test
    void shouldMergeWhenBuildingExistingGraphIfAllowed() {
        InMemoryKnowledgeGraphRepository repository = new InMemoryKnowledgeGraphRepository();
        KnowledgeGraphDomainService domainService = new KnowledgeGraphDomainService();
        BuildKnowledgeGraph buildKnowledgeGraph = new BuildKnowledgeGraph(repository, domainService, Clock.fixed(BASE_TIME, ZoneOffset.UTC));

        BuildKnowledgeGraphOutput initial = buildKnowledgeGraph.execute(new BuildKnowledgeGraphInput("mergeable", sampleExtractedKnowledge(List.of()), false));
        BuildKnowledgeGraphOutput merged = buildKnowledgeGraph.execute(new BuildKnowledgeGraphInput("mergeable", additionalKnowledge(), true));

        assertTrue(initial.success());
        assertTrue(merged.success());
        assertTrue(merged.wasExtended());
        assertNotNull(repository.findById(initial.graphId()).orElseThrow());
    }

    private static ExtractedKnowledge sampleExtractedKnowledge(List<String> warnings) {
        KnowledgeNode topic = new KnowledgeNode(
            new NodeId("node-topic"),
            "Knowledge Graph",
            NodeType.TOPIC,
            Map.of("domain", "retrieval"),
            SOURCE_DOCUMENT,
            ConfidenceScore.high(),
            BASE_TIME,
            BASE_TIME
        );
        KnowledgeNode concept = new KnowledgeNode(
            new NodeId("node-concept"),
            "Entity Linking",
            NodeType.CONCEPT,
            Map.of("mode", "semantic"),
            SOURCE_DOCUMENT,
            ConfidenceScore.medium(),
            BASE_TIME.plusSeconds(1),
            BASE_TIME.plusSeconds(1)
        );
        KnowledgeRelationship relationship = KnowledgeRelationship.create(
            topic.nodeId(),
            concept.nodeId(),
            RelationshipType.RELATED_TO,
            Map.of("evidence", "section-1"),
            SOURCE_DOCUMENT,
            ConfidenceScore.medium(),
            BASE_TIME.plusSeconds(2)
        );
        return new ExtractedKnowledge(
            List.of(topic, concept),
            List.of(relationship),
            SOURCE_DOCUMENT,
            new ExtractionMetadata("rule-engine", BASE_TIME.plusSeconds(3), Duration.ofSeconds(1), Map.of("defaultOption", true), warnings)
        );
    }

    private static ExtractedKnowledge additionalKnowledge() {
        KnowledgeNode existingTopicVariant = new KnowledgeNode(
            new NodeId("node-topic-variant"),
            " knowledge graph ",
            NodeType.TOPIC,
            Map.of("aliases", List.of("KG")),
            SOURCE_DOCUMENT,
            ConfidenceScore.high(),
            BASE_TIME.plusSeconds(4),
            BASE_TIME.plusSeconds(4)
        );
        KnowledgeNode relationshipNode = new KnowledgeNode(
            new NodeId("node-relationship"),
            "Document Upload",
            NodeType.EVENT,
            Map.of("step", "ingestion"),
            SOURCE_DOCUMENT,
            ConfidenceScore.medium(),
            BASE_TIME.plusSeconds(5),
            BASE_TIME.plusSeconds(5)
        );
        KnowledgeRelationship relationship = KnowledgeRelationship.create(
            existingTopicVariant.nodeId(),
            relationshipNode.nodeId(),
            RelationshipType.RELATED_TO,
            Map.of("evidence", "section-2"),
            SOURCE_DOCUMENT,
            ConfidenceScore.high(),
            BASE_TIME.plusSeconds(6)
        );
        return new ExtractedKnowledge(
            List.of(existingTopicVariant, relationshipNode),
            List.of(relationship),
            SOURCE_DOCUMENT,
            new ExtractionMetadata("llm", BASE_TIME.plusSeconds(7), Duration.ofSeconds(2), Map.of("temperature", 0.1d), List.of())
        );
    }

    private static final class StubDocumentQualityValidator implements DocumentQualityValidator {
        private final DocumentQualityResult result;

        private StubDocumentQualityValidator(DocumentQualityResult result) {
            this.result = result;
        }

        @Override
        public DocumentQualityResult validateForKnowledgeExtraction(String content, String documentType) {
            return result;
        }

        @Override
        public boolean hasMinimumContentLength(String content) {
            return content != null && content.length() >= 100;
        }

        @Override
        public boolean hasAcceptableLanguage(String content) {
            return true;
        }

        @Override
        public boolean hasStructuredContent(String content, String documentType) {
            return true;
        }
    }

    private static final class StubKnowledgeExtractionService implements KnowledgeExtractionService {
        private final ExtractedKnowledge response;
        private boolean supported = true;
        private RuntimeException exception;
        private Map<String, Object> lastOptions = Map.of();

        private StubKnowledgeExtractionService(ExtractedKnowledge response) {
            this.response = response;
        }

        @Override
        public ExtractedKnowledge extractKnowledge(String documentContent,
                                                  String documentTitle,
                                                  String documentType,
                                                  Map<String, Object> extractionOptions) throws KnowledgeExtractionException {
            lastOptions = extractionOptions;
            if (exception != null) {
                throw exception;
            }
            return response;
        }

        @Override
        public boolean supportsDocumentType(String documentType) {
            return supported;
        }

        @Override
        public List<String> getSupportedExtractionTypes() {
            return List.of("entities", "relationships");
        }

        @Override
        public Map<String, Object> getDefaultExtractionOptions() {
            return Map.of("defaultOption", true);
        }
    }

    private static final class InMemoryKnowledgeGraphRepository implements KnowledgeGraphRepository {
        private final Map<GraphId, KnowledgeGraph> graphs = new LinkedHashMap<>();

        @Override
        public KnowledgeGraph save(KnowledgeGraph knowledgeGraph) {
            graphs.put(knowledgeGraph.graphId(), knowledgeGraph);
            return knowledgeGraph;
        }

        @Override
        public Optional<KnowledgeGraph> findById(GraphId graphId) {
            return Optional.ofNullable(graphs.get(graphId));
        }

        @Override
        public Optional<KnowledgeGraph> findByName(String name) {
            return graphs.values().stream().filter(graph -> graph.name().equals(name)).findFirst();
        }

        @Override
        public List<KnowledgeGraph> findAll() {
            return new ArrayList<>(graphs.values());
        }

        @Override
        public void delete(GraphId graphId) {
            graphs.remove(graphId);
        }

        @Override
        public boolean existsByName(String name) {
            return findByName(name).isPresent();
        }

        @Override
        public List<KnowledgeNode> findNodesConnectedTo(NodeId nodeId) {
            return graphs.values().stream()
                .flatMap(graph -> graph.relationshipsFor(nodeId).stream())
                .flatMap(relationship -> List.of(relationship.fromNodeId(), relationship.toNodeId()).stream())
                .distinct()
                .map(this::findNodeById)
                .flatMap(Optional::stream)
                .toList();
        }

        @Override
        public List<KnowledgeRelationship> findRelationshipsByType(RelationshipType type) {
            return graphs.values().stream()
                .flatMap(graph -> graph.relationships().stream())
                .filter(relationship -> relationship.relationshipType() == type)
                .toList();
        }

        @Override
        public KnowledgeGraph findSubgraphAroundNode(NodeId nodeId, int depth) {
            return graphs.values().stream()
                .filter(graph -> graph.containsNode(nodeId))
                .findFirst()
                .orElseThrow();
        }

        private Optional<KnowledgeNode> findNodeById(NodeId nodeId) {
            return graphs.values().stream()
                .map(graph -> graph.getNode(nodeId))
                .flatMap(Optional::stream)
                .findFirst();
        }
    }

    private static final class TickingClock extends Clock {
        private Instant current;

        private TickingClock(Instant current) {
            this.current = current;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            Instant value = current;
            current = current.plusSeconds(1);
            return value;
        }
    }
}
