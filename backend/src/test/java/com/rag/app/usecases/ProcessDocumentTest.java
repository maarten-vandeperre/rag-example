package com.rag.app.usecases;

import com.rag.app.domain.entities.Document;
import com.rag.app.domain.valueobjects.DocumentMetadata;
import com.rag.app.domain.valueobjects.DocumentStatus;
import com.rag.app.domain.valueobjects.FileType;
import com.rag.app.shared.configuration.KnowledgeProcessingConfiguration;
import com.rag.app.shared.domain.knowledge.entities.KnowledgeGraph;
import com.rag.app.shared.domain.knowledge.entities.KnowledgeNode;
import com.rag.app.shared.domain.knowledge.services.KnowledgeGraphDomainService;
import com.rag.app.shared.domain.knowledge.valueobjects.ConfidenceScore;
import com.rag.app.shared.domain.knowledge.valueobjects.DocumentReference;
import com.rag.app.shared.domain.knowledge.valueobjects.ExtractedKnowledge;
import com.rag.app.shared.domain.knowledge.valueobjects.ExtractionMetadata;
import com.rag.app.shared.domain.knowledge.valueobjects.GraphId;
import com.rag.app.shared.domain.knowledge.valueobjects.NodeType;
import com.rag.app.shared.interfaces.knowledge.DocumentQualityResult;
import com.rag.app.shared.interfaces.knowledge.DocumentQualityValidator;
import com.rag.app.shared.interfaces.knowledge.KnowledgeExtractionException;
import com.rag.app.shared.interfaces.knowledge.KnowledgeExtractionService;
import com.rag.app.shared.interfaces.knowledge.KnowledgeGraphRepository;
import com.rag.app.shared.usecases.knowledge.BuildKnowledgeGraph;
import com.rag.app.shared.usecases.knowledge.ExtractKnowledgeFromDocument;
import com.rag.app.usecases.interfaces.VectorStore;
import com.rag.app.usecases.models.FailedDocumentInfo;
import com.rag.app.usecases.models.ProcessDocumentInput;
import com.rag.app.usecases.models.ProcessDocumentOutput;
import com.rag.app.usecases.models.ProcessingDocumentInfo;
import com.rag.app.usecases.models.ProcessingStatistics;
import com.rag.app.usecases.repositories.DocumentRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcessDocumentTest {

    @Test
    void shouldMarkDocumentReadyWhenProcessingSucceeds() {
        InMemoryDocumentRepository repository = new InMemoryDocumentRepository();
        Document document = uploadedDocument();
        repository.save(document);
        RecordingVectorStore vectorStore = new RecordingVectorStore();
        ProcessDocument useCase = new ProcessDocument(repository, (content, fileType) -> "Extracted text", vectorStore);

        ProcessDocumentOutput output = useCase.execute(new ProcessDocumentInput(document.documentId(), new byte[]{1, 2, 3}));

        assertEquals(DocumentStatus.READY, output.finalStatus());
        assertEquals(14, output.extractedTextLength());
        assertEquals(null, output.errorMessage());
        assertEquals(DocumentStatus.READY, repository.findById(document.documentId()).orElseThrow().status());
        assertEquals(document.documentId().toString(), vectorStore.documentId);
        assertEquals("Extracted text", vectorStore.text);
    }

    @Test
    void shouldCreateKnowledgeGraphWhenKnowledgeExtractionSucceeds() {
        InMemoryDocumentRepository repository = new InMemoryDocumentRepository();
        Document document = uploadedDocument();
        repository.save(document);
        RecordingVectorStore vectorStore = new RecordingVectorStore();
        InMemoryKnowledgeGraphRepository knowledgeGraphRepository = new InMemoryKnowledgeGraphRepository();
        ProcessDocument useCase = new ProcessDocument(
            repository,
            (content, fileType) -> "Ada Lovelace works on semantic search at Analytical Engine Labs.",
            vectorStore,
            extractKnowledgeFromDocument(),
            new BuildKnowledgeGraph(knowledgeGraphRepository, new KnowledgeGraphDomainService(), Clock.systemUTC()),
            new KnowledgeProcessingConfiguration()
        );

        ProcessDocumentOutput output = useCase.execute(new ProcessDocumentInput(document.documentId(), new byte[]{1, 2, 3}));

        assertEquals(DocumentStatus.READY, output.finalStatus());
        assertNotNull(knowledgeGraphRepository.savedGraph);
        assertTrue(knowledgeGraphRepository.savedGraph.name().startsWith("main-knowledge-graph-"));
        assertTrue(knowledgeGraphRepository.savedGraph.metadata().sourceDocuments().stream()
            .anyMatch(sourceDocument -> sourceDocument.documentName().equals(document.fileName())));
    }

    @Test
    void shouldKeepDocumentReadyWhenKnowledgeGraphCreationFails() {
        InMemoryDocumentRepository repository = new InMemoryDocumentRepository();
        Document document = uploadedDocument();
        repository.save(document);
        RecordingVectorStore vectorStore = new RecordingVectorStore();
        ProcessDocument useCase = new ProcessDocument(
            repository,
            (content, fileType) -> "Ada Lovelace works on semantic search at Analytical Engine Labs.",
            vectorStore,
            extractKnowledgeFromDocument(),
            new BuildKnowledgeGraph(new FailingKnowledgeGraphRepository(), new KnowledgeGraphDomainService(), Clock.systemUTC()),
            new KnowledgeProcessingConfiguration()
        );

        ProcessDocumentOutput output = useCase.execute(new ProcessDocumentInput(document.documentId(), new byte[]{1, 2, 3}));

        assertEquals(DocumentStatus.READY, output.finalStatus());
        assertEquals(DocumentStatus.READY, repository.findById(document.documentId()).orElseThrow().status());
        assertEquals(document.documentId().toString(), vectorStore.documentId);
    }

    @Test
    void shouldMarkDocumentFailedWhenExtractionReturnsNoUsableContent() {
        InMemoryDocumentRepository repository = new InMemoryDocumentRepository();
        Document document = uploadedDocument();
        repository.save(document);
        ProcessDocument useCase = new ProcessDocument(repository, (content, fileType) -> "   ", (documentId, text) -> { });

        ProcessDocumentOutput output = useCase.execute(new ProcessDocumentInput(document.documentId(), new byte[]{1}));

        assertEquals(DocumentStatus.FAILED, output.finalStatus());
        assertEquals(0, output.extractedTextLength());
        assertEquals("No usable content extracted from document", output.errorMessage());
        assertEquals(DocumentStatus.FAILED, repository.findById(document.documentId()).orElseThrow().status());
    }

    @Test
    void shouldMarkDocumentFailedWhenExtractionThrows() {
        InMemoryDocumentRepository repository = new InMemoryDocumentRepository();
        Document document = uploadedDocument();
        repository.save(document);
        ProcessDocument useCase = new ProcessDocument(repository, (content, fileType) -> {
            throw new IllegalStateException("Extraction failed");
        }, (documentId, text) -> { });

        ProcessDocumentOutput output = useCase.execute(new ProcessDocumentInput(document.documentId(), new byte[]{1}));

        assertEquals(DocumentStatus.FAILED, output.finalStatus());
        assertEquals("Extraction failed", output.errorMessage());
        assertEquals(DocumentStatus.FAILED, repository.findById(document.documentId()).orElseThrow().status());
    }

    @Test
    void shouldMarkDocumentFailedWhenVectorStorageThrows() {
        InMemoryDocumentRepository repository = new InMemoryDocumentRepository();
        Document document = uploadedDocument();
        repository.save(document);
        ProcessDocument useCase = new ProcessDocument(repository, (content, fileType) -> "Extracted text", (documentId, text) -> {
            throw new IllegalStateException("Vector storage failed");
        });

        ProcessDocumentOutput output = useCase.execute(new ProcessDocumentInput(document.documentId(), new byte[]{1}));

        assertEquals(DocumentStatus.FAILED, output.finalStatus());
        assertEquals("Vector storage failed", output.errorMessage());
        assertEquals(DocumentStatus.FAILED, repository.findById(document.documentId()).orElseThrow().status());
    }

    @Test
    void shouldRejectMissingDocumentId() {
        ProcessDocument useCase = new ProcessDocument(new InMemoryDocumentRepository(), (content, fileType) -> "text", (documentId, text) -> { });

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> useCase.execute(new ProcessDocumentInput(null, new byte[]{1})));

        assertEquals("documentId must not be null", exception.getMessage());
    }

    @Test
    void shouldRejectUnknownDocuments() {
        ProcessDocument useCase = new ProcessDocument(new InMemoryDocumentRepository(), (content, fileType) -> "text", (documentId, text) -> { });

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> useCase.execute(new ProcessDocumentInput(UUID.randomUUID(), new byte[]{1})));

        assertEquals("document must exist", exception.getMessage());
    }

    private static Document uploadedDocument() {
        return new Document(
            UUID.randomUUID(),
            new DocumentMetadata("guide.pdf", 123L, FileType.PDF, "hash-123"),
            UUID.randomUUID().toString(),
            Instant.parse("2026-03-13T15:00:00Z"),
            DocumentStatus.UPLOADED
        );
    }

    private static ExtractKnowledgeFromDocument extractKnowledgeFromDocument() {
        return new ExtractKnowledgeFromDocument(new StubKnowledgeExtractionService(), new PassingDocumentQualityValidator(), Clock.systemUTC());
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
            return documents.values().stream()
                .filter(document -> document.uploadedBy().equals(userId))
                .toList();
        }

        @Override
        public List<Document> findAll() {
            return documents.values().stream().toList();
        }

        @Override
        public List<Document> findByStatus(DocumentStatus status) {
            return documents.values().stream()
                .filter(document -> document.status() == status)
                .toList();
        }

        @Override
        public ProcessingStatistics getProcessingStatistics() {
            return new ProcessingStatistics(documents.size(), 0, 0, 0, 0);
        }

        @Override
        public List<FailedDocumentInfo> findFailedDocuments() {
            return List.of();
        }

        @Override
        public List<ProcessingDocumentInfo> findProcessingDocuments() {
            return List.of();
        }

        @Override
        public void updateStatus(UUID documentId, DocumentStatus status) {
            findById(documentId).ifPresent(document -> documents.put(documentId, document.withStatus(status)));
        }
    }

    private static final class RecordingVectorStore implements VectorStore {
        private String documentId;
        private String text;

        @Override
        public void storeDocumentVectors(String documentId, String text) {
            this.documentId = documentId;
            this.text = text;
        }
    }

    private static final class PassingDocumentQualityValidator implements DocumentQualityValidator {
        @Override
        public DocumentQualityResult validateForKnowledgeExtraction(String content, String documentType) {
            return DocumentQualityResult.sufficient(List.of());
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

    private static final class StubKnowledgeExtractionService implements KnowledgeExtractionService {
        @Override
        public ExtractedKnowledge extractKnowledge(String documentContent,
                                                  String documentTitle,
                                                  String documentType,
                                                  Map<String, Object> extractionOptions) throws KnowledgeExtractionException {
            Instant now = Instant.parse("2026-03-16T22:30:00Z");
            DocumentReference sourceDocument = new DocumentReference(
                UUID.nameUUIDFromBytes(documentTitle.getBytes()),
                documentTitle,
                null,
                1.0d
            );
            KnowledgeNode node = KnowledgeNode.create(
                "Ada Lovelace",
                NodeType.PERSON,
                Map.of("role", "engineer"),
                sourceDocument,
                ConfidenceScore.high(),
                now
            );
            return new ExtractedKnowledge(
                List.of(node),
                List.of(),
                sourceDocument,
                new ExtractionMetadata("stub", now, Duration.ofMillis(5), extractionOptions, List.of())
            );
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

    private static class InMemoryKnowledgeGraphRepository implements KnowledgeGraphRepository {
        private KnowledgeGraph savedGraph;

        @Override
        public KnowledgeGraph save(KnowledgeGraph knowledgeGraph) {
            this.savedGraph = knowledgeGraph;
            return knowledgeGraph;
        }

        @Override
        public Optional<KnowledgeGraph> findById(GraphId graphId) {
            return Optional.ofNullable(savedGraph).filter(graph -> graph.graphId().equals(graphId));
        }

        @Override
        public Optional<KnowledgeGraph> findByName(String name) {
            return Optional.ofNullable(savedGraph).filter(graph -> graph.name().equals(name));
        }

        @Override
        public List<KnowledgeGraph> findAll() {
            return savedGraph == null ? List.of() : List.of(savedGraph);
        }

        @Override
        public void delete(GraphId graphId) {
            if (savedGraph != null && savedGraph.graphId().equals(graphId)) {
                savedGraph = null;
            }
        }

        @Override
        public boolean existsByName(String name) {
            return savedGraph != null && savedGraph.name().equals(name);
        }

        @Override
        public List<com.rag.app.shared.domain.knowledge.entities.KnowledgeNode> findNodesConnectedTo(com.rag.app.shared.domain.knowledge.valueobjects.NodeId nodeId) {
            return List.of();
        }

        @Override
        public List<com.rag.app.shared.domain.knowledge.entities.KnowledgeRelationship> findRelationshipsByType(com.rag.app.shared.domain.knowledge.valueobjects.RelationshipType type) {
            return List.of();
        }

        @Override
        public KnowledgeGraph findSubgraphAroundNode(com.rag.app.shared.domain.knowledge.valueobjects.NodeId nodeId, int depth) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class FailingKnowledgeGraphRepository implements KnowledgeGraphRepository {
        @Override
        public KnowledgeGraph save(KnowledgeGraph knowledgeGraph) {
            throw new IllegalStateException("Neo4j unavailable");
        }

        @Override
        public Optional<KnowledgeGraph> findById(GraphId graphId) {
            return Optional.empty();
        }

        @Override
        public Optional<KnowledgeGraph> findByName(String name) {
            return Optional.empty();
        }

        @Override
        public List<KnowledgeGraph> findAll() {
            return List.of();
        }

        @Override
        public void delete(GraphId graphId) {
        }

        @Override
        public boolean existsByName(String name) {
            return false;
        }

        @Override
        public List<com.rag.app.shared.domain.knowledge.entities.KnowledgeNode> findNodesConnectedTo(com.rag.app.shared.domain.knowledge.valueobjects.NodeId nodeId) {
            return List.of();
        }

        @Override
        public List<com.rag.app.shared.domain.knowledge.entities.KnowledgeRelationship> findRelationshipsByType(com.rag.app.shared.domain.knowledge.valueobjects.RelationshipType type) {
            return List.of();
        }

        @Override
        public KnowledgeGraph findSubgraphAroundNode(com.rag.app.shared.domain.knowledge.valueobjects.NodeId nodeId, int depth) {
            throw new UnsupportedOperationException();
        }
    }
}
