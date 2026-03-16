package com.rag.app.document;

import com.rag.app.document.domain.entities.Document;
import com.rag.app.document.domain.entities.DocumentUser;
import com.rag.app.document.domain.services.DocumentDomainService;
import com.rag.app.document.domain.valueobjects.DocumentMetadata;
import com.rag.app.document.domain.valueobjects.DocumentStatus;
import com.rag.app.document.domain.valueobjects.FileType;
import com.rag.app.document.domain.valueobjects.UserRole;
import com.rag.app.document.infrastructure.DocumentManagementFacadeImpl;
import com.rag.app.document.interfaces.DocumentContentExtractor;
import com.rag.app.document.interfaces.DocumentRepository;
import com.rag.app.document.interfaces.DocumentStorage;
import com.rag.app.document.interfaces.DocumentUserDirectory;
import com.rag.app.document.interfaces.DocumentVectorStore;
import com.rag.app.document.usecases.GetAdminProgress;
import com.rag.app.document.usecases.GetUserDocuments;
import com.rag.app.document.usecases.ProcessDocument;
import com.rag.app.document.usecases.UploadDocument;
import com.rag.app.document.usecases.models.FailedDocumentInfo;
import com.rag.app.document.usecases.models.GetAdminProgressInput;
import com.rag.app.document.usecases.models.GetUserDocumentsInput;
import com.rag.app.document.usecases.models.ProcessingDocumentInfo;
import com.rag.app.document.usecases.models.ProcessingStatistics;
import com.rag.app.document.usecases.models.UploadDocumentInput;
import com.rag.app.shared.configuration.KnowledgeProcessingConfiguration;
import com.rag.app.shared.interfaces.knowledge.DocumentQualityResult;
import com.rag.app.shared.interfaces.knowledge.DocumentQualityValidator;
import com.rag.app.shared.interfaces.knowledge.KnowledgeExtractionException;
import com.rag.app.shared.interfaces.knowledge.KnowledgeExtractionService;
import com.rag.app.shared.interfaces.knowledge.KnowledgeGraphRepository;
import com.rag.app.shared.usecases.knowledge.BuildKnowledgeGraph;
import com.rag.app.shared.usecases.knowledge.ExtractKnowledgeFromDocument;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DocumentManagementFacadeTest {
    @Test
    void shouldRunDocumentLifecycleThroughFacade() {
        InMemoryDocumentRepository repository = new InMemoryDocumentRepository();
        InMemoryDocumentStorage storage = new InMemoryDocumentStorage();
        InMemoryUserDirectory users = new InMemoryUserDirectory();
        users.store(new DocumentUser("admin-1", "admin", UserRole.ADMIN, true));
        users.store(new DocumentUser("user-1", "user", UserRole.STANDARD, true));
        RecordingVectorStore vectorStore = new RecordingVectorStore();
        Clock clock = Clock.fixed(Instant.parse("2026-03-14T10:15:30Z"), ZoneOffset.UTC);
        DocumentDomainService domainService = new DocumentDomainService();

        DocumentManagementFacadeImpl facade = new DocumentManagementFacadeImpl(
            new UploadDocument(repository, users, storage, domainService, clock),
            new ProcessDocument(
                repository,
                storage,
                new StubExtractor(),
                vectorStore,
                new ExtractKnowledgeFromDocument(new StubKnowledgeExtractionService(), new StubDocumentQualityValidator(), clock),
                new BuildKnowledgeGraph(new InMemoryKnowledgeGraphRepository(), new com.rag.app.shared.domain.knowledge.services.KnowledgeGraphDomainService(), clock),
                new KnowledgeProcessingConfiguration(),
                clock
            ),
            new GetUserDocuments(repository, users, domainService),
            new GetAdminProgress(repository, users, domainService),
            repository
        );

        var upload = facade.uploadDocument(new UploadDocumentInput(
            "guide.md",
            12L,
            FileType.MARKDOWN,
            "hello module".getBytes(),
            "user-1"
        ));

        assertNotNull(upload.documentId());
        assertEquals(DocumentStatus.UPLOADED, upload.status());

        var process = facade.processDocument(new com.rag.app.document.usecases.models.ProcessDocumentInput(upload.documentId()));

        assertEquals(DocumentStatus.READY, process.finalStatus());
        assertEquals("hello module", vectorStore.lastText);
        assertNotNull(process.knowledgeProcessingStatus());

        var userDocuments = facade.getUserDocuments(new GetUserDocumentsInput("user-1", false));
        assertEquals(1, userDocuments.totalCount());
        assertEquals("guide.md", userDocuments.documents().get(0).fileName());

        repository.failedDocuments = List.of(new FailedDocumentInfo("doc-2", "broken.pdf", "user-1",
            Instant.parse("2026-03-14T09:00:00Z"), "Parser error", 22L));
        repository.processingDocuments = List.of(new ProcessingDocumentInfo("doc-3", "queued.txt", "user-1",
            Instant.parse("2026-03-14T08:00:00Z"), Instant.parse("2026-03-14T08:10:00Z")));
        repository.processingStatistics = new ProcessingStatistics(3, 0, 1, 1, 1);

        var adminProgress = facade.getAdminProgress(new GetAdminProgressInput("admin-1"));
        assertEquals(new ProcessingStatistics(3, 0, 1, 1, 1), adminProgress.processingStatistics());
        assertEquals(1, adminProgress.failedDocuments().size());
        assertEquals(1, adminProgress.processingDocuments().size());

        Optional<Document> document = facade.findDocumentById(upload.documentId().toString());
        assertEquals(DocumentStatus.READY, document.orElseThrow().status());
        assertEquals(1, facade.findDocumentsByUser("user-1").size());
    }

    private static final class StubDocumentQualityValidator implements DocumentQualityValidator {
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
        public com.rag.app.shared.domain.knowledge.valueobjects.ExtractedKnowledge extractKnowledge(String documentContent,
                                                                                                    String documentTitle,
                                                                                                    String documentType,
                                                                                                    Map<String, Object> extractionOptions) throws KnowledgeExtractionException {
            var sourceDocument = new com.rag.app.shared.domain.knowledge.valueobjects.DocumentReference(
                UUID.nameUUIDFromBytes(documentTitle.getBytes()),
                documentTitle,
                null,
                0.75d
            );
            return new com.rag.app.shared.domain.knowledge.valueobjects.ExtractedKnowledge(
                List.of(),
                List.of(),
                sourceDocument,
                new com.rag.app.shared.domain.knowledge.valueobjects.ExtractionMetadata(
                    "stub",
                    Instant.parse("2026-03-14T10:15:30Z"),
                    Duration.ofMillis(1),
                    extractionOptions,
                    List.of()
                )
            );
        }

        @Override
        public boolean supportsDocumentType(String documentType) {
            return true;
        }

        @Override
        public List<String> getSupportedExtractionTypes() {
            return List.of("entities");
        }

        @Override
        public Map<String, Object> getDefaultExtractionOptions() {
            return Map.of();
        }
    }

    private static final class InMemoryKnowledgeGraphRepository implements KnowledgeGraphRepository {
        private final Map<com.rag.app.shared.domain.knowledge.valueobjects.GraphId, com.rag.app.shared.domain.knowledge.entities.KnowledgeGraph> graphs = new ConcurrentHashMap<>();

        @Override
        public com.rag.app.shared.domain.knowledge.entities.KnowledgeGraph save(com.rag.app.shared.domain.knowledge.entities.KnowledgeGraph knowledgeGraph) {
            graphs.put(knowledgeGraph.graphId(), knowledgeGraph);
            return knowledgeGraph;
        }

        @Override
        public Optional<com.rag.app.shared.domain.knowledge.entities.KnowledgeGraph> findById(com.rag.app.shared.domain.knowledge.valueobjects.GraphId graphId) {
            return Optional.ofNullable(graphs.get(graphId));
        }

        @Override
        public Optional<com.rag.app.shared.domain.knowledge.entities.KnowledgeGraph> findByName(String name) {
            return graphs.values().stream().filter(graph -> graph.name().equals(name)).findFirst();
        }

        @Override
        public List<com.rag.app.shared.domain.knowledge.entities.KnowledgeGraph> findAll() {
            return graphs.values().stream().toList();
        }

        @Override
        public void delete(com.rag.app.shared.domain.knowledge.valueobjects.GraphId graphId) {
            graphs.remove(graphId);
        }

        @Override
        public boolean existsByName(String name) {
            return findByName(name).isPresent();
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
        public com.rag.app.shared.domain.knowledge.entities.KnowledgeGraph findSubgraphAroundNode(com.rag.app.shared.domain.knowledge.valueobjects.NodeId nodeId, int depth) {
            throw new IllegalArgumentException("not implemented");
        }
    }

    private static final class StubExtractor implements DocumentContentExtractor {
        @Override
        public String extractText(byte[] fileContent, FileType fileType) {
            return new String(fileContent);
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
            content.put(documentId, fileContent.clone());
        }

        @Override
        public Optional<byte[]> load(UUID documentId) {
            return Optional.ofNullable(content.get(documentId));
        }
    }

    private static final class InMemoryUserDirectory implements DocumentUserDirectory {
        private final Map<String, DocumentUser> users = new ConcurrentHashMap<>();

        @Override
        public Optional<DocumentUser> findById(String userId) {
            return Optional.ofNullable(users.get(userId));
        }

        void store(DocumentUser user) {
            users.put(user.userId(), user);
        }
    }

    private static final class InMemoryDocumentRepository implements DocumentRepository {
        private final Map<UUID, Document> documents = new ConcurrentHashMap<>();
        private ProcessingStatistics processingStatistics = new ProcessingStatistics(0, 0, 0, 0, 0);
        private List<FailedDocumentInfo> failedDocuments = List.of();
        private List<ProcessingDocumentInfo> processingDocuments = List.of();

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
            return processingStatistics;
        }

        @Override
        public List<FailedDocumentInfo> findFailedDocuments() {
            return failedDocuments;
        }

        @Override
        public List<ProcessingDocumentInfo> findProcessingDocuments() {
            return processingDocuments;
        }
    }
}
