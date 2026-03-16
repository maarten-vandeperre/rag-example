package com.rag.app.document.usecases;

import com.rag.app.document.domain.entities.Document;
import com.rag.app.document.domain.valueobjects.DocumentMetadata;
import com.rag.app.document.domain.valueobjects.DocumentStatus;
import com.rag.app.document.domain.valueobjects.FileType;
import com.rag.app.document.domain.valueobjects.KnowledgeProcessingStatus;
import com.rag.app.document.interfaces.DocumentContentExtractor;
import com.rag.app.document.interfaces.DocumentRepository;
import com.rag.app.document.interfaces.DocumentStorage;
import com.rag.app.document.interfaces.DocumentVectorStore;
import com.rag.app.document.usecases.models.FailedDocumentInfo;
import com.rag.app.document.usecases.models.KnowledgeProcessingResult;
import com.rag.app.document.usecases.models.ProcessDocumentInput;
import com.rag.app.document.usecases.models.ProcessingDocumentInfo;
import com.rag.app.document.usecases.models.ProcessingStatistics;
import com.rag.app.shared.configuration.KnowledgeProcessingConfiguration;
import com.rag.app.shared.domain.knowledge.entities.KnowledgeGraph;
import com.rag.app.shared.domain.knowledge.entities.KnowledgeNode;
import com.rag.app.shared.domain.knowledge.entities.KnowledgeRelationship;
import com.rag.app.shared.domain.knowledge.services.KnowledgeGraphDomainService;
import com.rag.app.shared.domain.knowledge.valueobjects.ConfidenceScore;
import com.rag.app.shared.domain.knowledge.valueobjects.DocumentReference;
import com.rag.app.shared.domain.knowledge.valueobjects.ExtractedKnowledge;
import com.rag.app.shared.domain.knowledge.valueobjects.ExtractionMetadata;
import com.rag.app.shared.domain.knowledge.valueobjects.GraphId;
import com.rag.app.shared.domain.knowledge.valueobjects.NodeId;
import com.rag.app.shared.domain.knowledge.valueobjects.NodeType;
import com.rag.app.shared.domain.knowledge.valueobjects.RelationshipType;
import com.rag.app.shared.interfaces.knowledge.DocumentQualityResult;
import com.rag.app.shared.interfaces.knowledge.DocumentQualityValidator;
import com.rag.app.shared.interfaces.knowledge.KnowledgeExtractionException;
import com.rag.app.shared.interfaces.knowledge.KnowledgeExtractionService;
import com.rag.app.shared.interfaces.knowledge.KnowledgeGraphRepository;
import com.rag.app.shared.usecases.knowledge.BuildKnowledgeGraph;
import com.rag.app.shared.usecases.knowledge.ExtractKnowledgeFromDocument;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcessDocumentKnowledgeIntegrationTest {
    private static final Instant NOW = Instant.parse("2026-03-16T14:00:00Z");

    @Test
    void shouldProcessSearchAndKnowledgeInParallelAndMarkReady() {
        InMemoryDocumentRepository repository = new InMemoryDocumentRepository();
        InMemoryDocumentStorage storage = new InMemoryDocumentStorage();
        RecordingVectorStore vectorStore = new RecordingVectorStore();
        UUID documentId = UUID.fromString("00000000-0000-0000-0000-000000000069");
        Document document = uploadedDocument(documentId, FileType.MARKDOWN);
        repository.save(document);
        storage.store(documentId, "Knowledge graph content for processing".repeat(5).getBytes(StandardCharsets.UTF_8));

        ProcessDocument useCase = new ProcessDocument(
            repository,
            storage,
            (bytes, fileType) -> new String(bytes, StandardCharsets.UTF_8),
            vectorStore,
            new ExtractKnowledgeFromDocument(new StubKnowledgeExtractionService(sampleKnowledge(List.of("weak relationship")), null), new StubDocumentQualityValidator(DocumentQualityResult.sufficient(List.of())), Clock.fixed(NOW, ZoneOffset.UTC)),
            new BuildKnowledgeGraph(new InMemoryKnowledgeGraphRepository(), new KnowledgeGraphDomainService(), Clock.fixed(NOW, ZoneOffset.UTC)),
            new KnowledgeProcessingConfiguration(),
            Clock.fixed(NOW, ZoneOffset.UTC)
        );

        var output = useCase.execute(new ProcessDocumentInput(documentId));
        Document saved = repository.findById(documentId).orElseThrow();

        assertEquals(DocumentStatus.READY, output.finalStatus());
        assertEquals(KnowledgeProcessingStatus.COMPLETED_WITH_WARNINGS, output.knowledgeProcessingStatus());
        assertTrue(output.knowledgeProcessingWarnings().contains("weak relationship"));
        assertEquals("Knowledge graph content for processing".repeat(5), vectorStore.lastText);
        assertTrue(saved.isKnowledgeProcessingComplete());
        assertNull(output.errorMessage());
    }

    @Test
    void shouldKeepDocumentReadyWhenKnowledgeProcessingFails() {
        InMemoryDocumentRepository repository = new InMemoryDocumentRepository();
        InMemoryDocumentStorage storage = new InMemoryDocumentStorage();
        UUID documentId = UUID.randomUUID();
        repository.save(uploadedDocument(documentId, FileType.MARKDOWN));
        storage.store(documentId, "knowledge content".repeat(20).getBytes(StandardCharsets.UTF_8));

        ProcessDocument useCase = new ProcessDocument(
            repository,
            storage,
            (bytes, fileType) -> new String(bytes, StandardCharsets.UTF_8),
            (docId, text) -> { },
            new ExtractKnowledgeFromDocument(
                new StubKnowledgeExtractionService(sampleKnowledge(List.of()), new KnowledgeExtractionException("extractor unavailable")),
                new StubDocumentQualityValidator(DocumentQualityResult.sufficient(List.of())),
                Clock.fixed(NOW, ZoneOffset.UTC)
            ),
            new BuildKnowledgeGraph(new InMemoryKnowledgeGraphRepository(), new KnowledgeGraphDomainService(), Clock.fixed(NOW, ZoneOffset.UTC)),
            new KnowledgeProcessingConfiguration(),
            Clock.fixed(NOW, ZoneOffset.UTC)
        );

        var output = useCase.execute(new ProcessDocumentInput(documentId));

        assertEquals(DocumentStatus.READY, output.finalStatus());
        assertEquals(KnowledgeProcessingStatus.FAILED, output.knowledgeProcessingStatus());
        assertTrue(output.knowledgeProcessingError().contains("Knowledge extraction failed"));
        assertNull(output.errorMessage());
    }

    @Test
    void shouldSkipKnowledgeProcessingWhenDisabledByConfiguration() {
        InMemoryDocumentRepository repository = new InMemoryDocumentRepository();
        InMemoryDocumentStorage storage = new InMemoryDocumentStorage();
        UUID documentId = UUID.randomUUID();
        repository.save(uploadedDocument(documentId, FileType.PLAIN_TEXT));
        storage.store(documentId, "plain text content".repeat(20).getBytes(StandardCharsets.UTF_8));

        KnowledgeProcessingConfiguration configuration = new KnowledgeProcessingConfiguration(
            Map.of("PLAIN_TEXT", false),
            Map.of(),
            "custom-graph"
        );

        ProcessDocument useCase = new ProcessDocument(
            repository,
            storage,
            (bytes, fileType) -> new String(bytes, StandardCharsets.UTF_8),
            (docId, text) -> { },
            new ExtractKnowledgeFromDocument(new StubKnowledgeExtractionService(sampleKnowledge(List.of()), null), new StubDocumentQualityValidator(DocumentQualityResult.sufficient(List.of())), Clock.fixed(NOW, ZoneOffset.UTC)),
            new BuildKnowledgeGraph(new InMemoryKnowledgeGraphRepository(), new KnowledgeGraphDomainService(), Clock.fixed(NOW, ZoneOffset.UTC)),
            configuration,
            Clock.fixed(NOW, ZoneOffset.UTC)
        );

        var output = useCase.execute(new ProcessDocumentInput(documentId));

        assertEquals(DocumentStatus.READY, output.finalStatus());
        assertEquals(KnowledgeProcessingStatus.DISABLED, output.knowledgeProcessingStatus());
        assertTrue(output.knowledgeProcessingWarnings().get(0).contains("disabled"));
    }

    @Test
    void shouldFailDocumentWhenSearchProcessingFails() {
        InMemoryDocumentRepository repository = new InMemoryDocumentRepository();
        InMemoryDocumentStorage storage = new InMemoryDocumentStorage();
        UUID documentId = UUID.randomUUID();
        repository.save(uploadedDocument(documentId, FileType.MARKDOWN));
        storage.store(documentId, "markdown content".repeat(20).getBytes(StandardCharsets.UTF_8));

        ProcessDocument useCase = new ProcessDocument(
            repository,
            storage,
            (bytes, fileType) -> new String(bytes, StandardCharsets.UTF_8),
            (docId, text) -> { throw new IllegalStateException("vector store offline"); },
            new ExtractKnowledgeFromDocument(new StubKnowledgeExtractionService(sampleKnowledge(List.of()), null), new StubDocumentQualityValidator(DocumentQualityResult.sufficient(List.of())), Clock.fixed(NOW, ZoneOffset.UTC)),
            new BuildKnowledgeGraph(new InMemoryKnowledgeGraphRepository(), new KnowledgeGraphDomainService(), Clock.fixed(NOW, ZoneOffset.UTC)),
            new KnowledgeProcessingConfiguration(),
            Clock.fixed(NOW, ZoneOffset.UTC)
        );

        var output = useCase.execute(new ProcessDocumentInput(documentId));

        assertEquals(DocumentStatus.FAILED, output.finalStatus());
        assertTrue(output.errorMessage().contains("Search processing failed"));
        assertTrue(output.knowledgeProcessingStatus().isSuccessful());
    }

    @Test
    void shouldRetryTransientKnowledgeProcessingFailuresAndSucceed() {
        InMemoryDocumentRepository repository = new InMemoryDocumentRepository();
        InMemoryDocumentStorage storage = new InMemoryDocumentStorage();
        UUID documentId = UUID.randomUUID();
        repository.save(uploadedDocument(documentId, FileType.MARKDOWN));
        storage.store(documentId, "markdown content".repeat(20).getBytes(StandardCharsets.UTF_8));

        AtomicInteger attempts = new AtomicInteger();
        ProcessDocument useCase = new ProcessDocument(
            repository,
            storage,
            (bytes, fileType) -> new String(bytes, StandardCharsets.UTF_8),
            (docId, text) -> { },
            new ExtractKnowledgeFromDocument(new StubKnowledgeExtractionService(sampleKnowledge(List.of()), null) {
                @Override
                public ExtractedKnowledge extractKnowledge(String documentContent, String documentTitle, String documentType, Map<String, Object> extractionOptions) {
                    if (attempts.getAndIncrement() == 0) {
                        throw new KnowledgeExtractionException("extractor temporarily unavailable");
                    }
                    return super.extractKnowledge(documentContent, documentTitle, documentType, extractionOptions);
                }
            }, new StubDocumentQualityValidator(DocumentQualityResult.sufficient(List.of())), Clock.fixed(NOW, ZoneOffset.UTC)),
            new BuildKnowledgeGraph(new InMemoryKnowledgeGraphRepository(), new KnowledgeGraphDomainService(), Clock.fixed(NOW, ZoneOffset.UTC)),
            new KnowledgeProcessingConfiguration(Map.of("MARKDOWN", true), Map.of(), "retry-graph", Duration.ofSeconds(1), 1),
            Clock.fixed(NOW, ZoneOffset.UTC)
        );

        var output = useCase.execute(new ProcessDocumentInput(documentId));

        assertEquals(DocumentStatus.READY, output.finalStatus());
        assertTrue(output.knowledgeProcessingStatus().isSuccessful());
        assertEquals(2, attempts.get());
    }

    @Test
    void shouldTimeoutKnowledgeProcessingAndKeepDocumentSearchable() {
        InMemoryDocumentRepository repository = new InMemoryDocumentRepository();
        InMemoryDocumentStorage storage = new InMemoryDocumentStorage();
        UUID documentId = UUID.randomUUID();
        repository.save(uploadedDocument(documentId, FileType.MARKDOWN));
        storage.store(documentId, "markdown content".repeat(20).getBytes(StandardCharsets.UTF_8));

        ProcessDocument useCase = new ProcessDocument(
            repository,
            storage,
            (bytes, fileType) -> new String(bytes, StandardCharsets.UTF_8),
            (docId, text) -> { },
            new ExtractKnowledgeFromDocument(new StubKnowledgeExtractionService(sampleKnowledge(List.of()), null) {
                @Override
                public ExtractedKnowledge extractKnowledge(String documentContent, String documentTitle, String documentType, Map<String, Object> extractionOptions) {
                    try {
                        Thread.sleep(150);
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                    }
                    return super.extractKnowledge(documentContent, documentTitle, documentType, extractionOptions);
                }
            }, new StubDocumentQualityValidator(DocumentQualityResult.sufficient(List.of())), Clock.fixed(NOW, ZoneOffset.UTC)),
            new BuildKnowledgeGraph(new InMemoryKnowledgeGraphRepository(), new KnowledgeGraphDomainService(), Clock.fixed(NOW, ZoneOffset.UTC)),
            new KnowledgeProcessingConfiguration(Map.of("MARKDOWN", true), Map.of(), "timeout-graph", Duration.ofMillis(25), 0),
            Clock.fixed(NOW, ZoneOffset.UTC)
        );

        var output = useCase.execute(new ProcessDocumentInput(documentId));

        assertEquals(DocumentStatus.READY, output.finalStatus());
        assertEquals(KnowledgeProcessingStatus.FAILED, output.knowledgeProcessingStatus());
        assertTrue(output.knowledgeProcessingError().contains("timed out"));
        assertNull(output.errorMessage());
    }

    private static Document uploadedDocument(UUID documentId, FileType fileType) {
        return new Document(
            documentId,
            new DocumentMetadata("doc." + fileType.name().toLowerCase(), 100L, fileType, "hash-" + documentId),
            "user-1",
            NOW,
            NOW,
            DocumentStatus.UPLOADED
        );
    }

    private static ExtractedKnowledge sampleKnowledge(List<String> warnings) {
        DocumentReference reference = new DocumentReference(UUID.randomUUID(), "doc.md", null, 0.9d);
        KnowledgeNode topic = new KnowledgeNode(new NodeId("topic-1"), "Knowledge Graph", NodeType.TOPIC, Map.of(), reference, ConfidenceScore.high(), NOW, NOW);
        KnowledgeNode concept = new KnowledgeNode(new NodeId("concept-1"), "Parallel Processing", NodeType.CONCEPT, Map.of(), reference, ConfidenceScore.medium(), NOW.plusSeconds(1), NOW.plusSeconds(1));
        KnowledgeRelationship relationship = KnowledgeRelationship.create(topic.nodeId(), concept.nodeId(), RelationshipType.RELATED_TO, Map.of(), reference, ConfidenceScore.medium(), NOW.plusSeconds(2));
        return new ExtractedKnowledge(
            List.of(topic, concept),
            List.of(relationship),
            reference,
            new ExtractionMetadata("stub", NOW.plusSeconds(3), Duration.ofSeconds(1), Map.of(), warnings)
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
            return true;
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

    private static class StubKnowledgeExtractionService implements KnowledgeExtractionService {
        private final ExtractedKnowledge extractedKnowledge;
        private final RuntimeException exception;

        private StubKnowledgeExtractionService(ExtractedKnowledge extractedKnowledge, RuntimeException exception) {
            this.extractedKnowledge = extractedKnowledge;
            this.exception = exception;
        }

        @Override
        public ExtractedKnowledge extractKnowledge(String documentContent, String documentTitle, String documentType, Map<String, Object> extractionOptions) {
            if (exception != null) {
                throw exception;
            }
            return extractedKnowledge;
        }

        @Override
        public boolean supportsDocumentType(String documentType) {
            return true;
        }

        @Override
        public List<String> getSupportedExtractionTypes() {
            return List.of("entities", "relationships");
        }

        @Override
        public Map<String, Object> getDefaultExtractionOptions() {
            return Map.of();
        }
    }

    private static final class RecordingVectorStore implements DocumentVectorStore {
        private String lastText;

        @Override
        public void storeDocument(UUID documentId, String text) {
            this.lastText = text;
        }
    }

    private static final class InMemoryDocumentStorage implements DocumentStorage {
        private final Map<UUID, byte[]> content = new ConcurrentHashMap<>();

        @Override
        public void store(UUID documentId, byte[] fileContent) {
            content.put(documentId, fileContent);
        }

        @Override
        public Optional<byte[]> load(UUID documentId) {
            return Optional.ofNullable(content.get(documentId));
        }
    }

    private static final class InMemoryDocumentRepository implements DocumentRepository {
        private final Map<UUID, Document> documents = new ConcurrentHashMap<>();

        @Override
        public Document save(Document document) {
            documents.put(document.documentId(), document);
            return document;
        }

        @Override
        public Optional<Document> findByContentHash(String hash) {
            return documents.values().stream().filter(document -> document.contentHash().equals(hash)).findFirst();
        }

        @Override
        public Optional<Document> findById(UUID documentId) {
            return Optional.ofNullable(documents.get(documentId));
        }

        @Override
        public List<Document> findByUploadedBy(String userId) {
            return documents.values().stream().filter(document -> document.uploadedBy().equals(userId)).toList();
        }

        @Override
        public List<Document> findAll() {
            return documents.values().stream().toList();
        }

        @Override
        public List<Document> findByStatus(DocumentStatus status) {
            return documents.values().stream().filter(document -> document.status() == status).toList();
        }

        @Override
        public ProcessingStatistics getProcessingStatistics() {
            return new ProcessingStatistics(0, 0, 0, 0, 0);
        }

        @Override
        public List<FailedDocumentInfo> findFailedDocuments() {
            return List.of();
        }

        @Override
        public List<ProcessingDocumentInfo> findProcessingDocuments() {
            return List.of();
        }
    }

    private static final class InMemoryKnowledgeGraphRepository implements KnowledgeGraphRepository {
        private final Map<GraphId, KnowledgeGraph> graphs = new ConcurrentHashMap<>();

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
            return graphs.values().stream().toList();
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
            return List.of();
        }

        @Override
        public List<KnowledgeRelationship> findRelationshipsByType(RelationshipType type) {
            return List.of();
        }

        @Override
        public KnowledgeGraph findSubgraphAroundNode(NodeId nodeId, int depth) {
            return graphs.values().stream().findFirst().orElseThrow();
        }
    }
}
