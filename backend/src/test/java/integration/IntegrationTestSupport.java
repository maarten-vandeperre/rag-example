package integration;

import com.rag.app.api.ChatController;
import com.rag.app.api.DocumentLibraryResource;
import com.rag.app.api.DocumentUploadController;
import com.rag.app.api.KnowledgeGraphResource;
import com.rag.app.api.dto.AdminProgressResponse;
import com.rag.app.api.dto.ChatQueryRequest;
import com.rag.app.api.dto.ChatQueryResponse;
import com.rag.app.api.dto.DocumentListResponse;
import com.rag.app.api.dto.UploadDocumentRequest;
import com.rag.app.api.dto.UploadDocumentResponse;
import com.rag.app.domain.entities.Document;
import com.rag.app.domain.entities.User;
import com.rag.app.domain.valueobjects.DocumentStatus;
import com.rag.app.domain.valueobjects.FileType;
import com.rag.app.domain.valueobjects.UserRole;
import com.rag.app.infrastructure.document.DocumentContentExtractorImpl;
import com.rag.app.infrastructure.llm.AnswerGeneratorImpl;
import com.rag.app.infrastructure.llm.HeuristicLlmClient;
import com.rag.app.infrastructure.llm.PromptTemplate;
import com.rag.app.infrastructure.llm.ResponseValidator;
import com.rag.app.infrastructure.vector.EmbeddingGenerator;
import com.rag.app.infrastructure.vector.TextChunker;
import com.rag.app.infrastructure.vector.VectorStoreImpl;
import com.rag.app.integration.api.controllers.KnowledgeGraphController;
import com.rag.app.integration.api.dto.knowledge.KnowledgeGraphDtoMapper;
import com.rag.app.shared.configuration.KnowledgeProcessingConfiguration;
import com.rag.app.shared.domain.knowledge.entities.KnowledgeGraph;
import com.rag.app.shared.domain.knowledge.entities.KnowledgeNode;
import com.rag.app.shared.domain.knowledge.entities.KnowledgeRelationship;
import com.rag.app.shared.domain.knowledge.services.KnowledgeGraphDomainService;
import com.rag.app.shared.domain.knowledge.valueobjects.GraphId;
import com.rag.app.shared.domain.knowledge.valueobjects.NodeId;
import com.rag.app.shared.domain.knowledge.valueobjects.RelationshipType;
import com.rag.app.shared.infrastructure.knowledge.HeuristicDocumentQualityValidator;
import com.rag.app.shared.infrastructure.knowledge.HeuristicKnowledgeExtractionService;
import com.rag.app.shared.interfaces.knowledge.KnowledgeGraphRepository;
import com.rag.app.shared.usecases.knowledge.BrowseKnowledgeGraph;
import com.rag.app.shared.usecases.knowledge.BuildKnowledgeGraph;
import com.rag.app.shared.usecases.knowledge.ExtractKnowledgeFromDocument;
import com.rag.app.shared.usecases.knowledge.GetKnowledgeGraphStatistics;
import com.rag.app.shared.usecases.knowledge.SearchKnowledgeGraph;
import com.rag.app.usecases.GetAdminProgress;
import com.rag.app.usecases.GetUserDocuments;
import com.rag.app.usecases.ProcessDocument;
import com.rag.app.usecases.QueryDocuments;
import com.rag.app.usecases.UploadDocument;
import com.rag.app.usecases.models.FailedDocumentInfo;
import com.rag.app.usecases.models.ProcessDocumentInput;
import com.rag.app.usecases.models.ProcessDocumentOutput;
import com.rag.app.usecases.models.ProcessingDocumentInfo;
import com.rag.app.usecases.models.ProcessingStatistics;
import com.rag.app.usecases.repositories.DocumentRepository;
import com.rag.app.usecases.repositories.UserRepository;
import com.rag.app.user.domain.valueobjects.UserId;
import com.rag.app.user.interfaces.UserManagementFacade;
import com.rag.app.user.usecases.models.AuthenticationRequest;
import com.rag.app.user.usecases.models.AuthenticationResult;
import com.rag.app.user.usecases.models.GetUserProfileInput;
import com.rag.app.user.usecases.models.GetUserProfileOutput;
import com.rag.app.user.usecases.models.ManageUserRolesInput;
import com.rag.app.user.usecases.models.ManageUserRolesOutput;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Principal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.LinkedHashMap;

final class IntegrationTestSupport {
    static final UUID STANDARD_USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    static final UUID OTHER_USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    static final UUID ADMIN_USER_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");
    final InMemoryDocumentRepository documentRepository = new InMemoryDocumentRepository();
    final InMemoryUserRepository userRepository = new InMemoryUserRepository();
    final DocumentUploadController documentUploadController;
    final ProcessDocument processDocument;
    final DocumentLibraryResource documentLibraryResource;
    final ChatController chatController;
    final KnowledgeGraphResource knowledgeGraphResource;

    IntegrationTestSupport() {
        Clock clock = Clock.fixed(Instant.parse("2026-03-14T10:00:00Z"), ZoneOffset.UTC);
        userRepository.store(user(STANDARD_USER_ID, UserRole.STANDARD));
        userRepository.store(user(OTHER_USER_ID, UserRole.STANDARD));
        userRepository.store(user(ADMIN_USER_ID, UserRole.ADMIN));

        VectorStoreImpl vectorStore = new VectorStoreImpl(documentRepository, new TextChunker(), new EmbeddingGenerator());
        InMemoryKnowledgeGraphRepository knowledgeGraphRepository = new InMemoryKnowledgeGraphRepository();
        this.processDocument = new ProcessDocument(
            documentRepository,
            new DocumentContentExtractorImpl(),
            vectorStore,
            new ExtractKnowledgeFromDocument(new HeuristicKnowledgeExtractionService(), new HeuristicDocumentQualityValidator(), clock),
            new BuildKnowledgeGraph(knowledgeGraphRepository, new KnowledgeGraphDomainService(), clock),
            new KnowledgeProcessingConfiguration()
        );
        UploadDocument uploadDocument = new UploadDocument(documentRepository, userRepository, this.processDocument, clock);
        QueryDocuments queryDocuments = new QueryDocuments(
            userRepository,
            documentRepository,
            vectorStore,
            new AnswerGeneratorImpl(new PromptTemplate(), new HeuristicLlmClient(), new ResponseValidator()),
            clock
        );

        this.documentUploadController = new DocumentUploadController(uploadDocument);
        this.documentLibraryResource = new DocumentLibraryResource(
            new GetUserDocuments(documentRepository, userRepository),
            new GetAdminProgress(documentRepository, userRepository)
        );
        this.chatController = new ChatController(queryDocuments);
        this.knowledgeGraphResource = new KnowledgeGraphResource(
            new KnowledgeGraphController(
                new BrowseKnowledgeGraph(knowledgeGraphRepository),
                new SearchKnowledgeGraph(knowledgeGraphRepository),
                new GetKnowledgeGraphStatistics(knowledgeGraphRepository),
                new KnowledgeGraphDtoMapper(),
                new StubUserManagementFacade()
            )
        );
    }

    UploadDocumentResponse upload(UUID userId, String fileName, String contentType, byte[] bytes) throws IOException {
        UploadDocumentRequest request = new UploadDocumentRequest();
        request.userId = userId.toString();
        Path path = Files.createTempFile("integration-upload-", suffix(fileName));
        Files.write(path, bytes);
        request.file = new StubFileUpload(path, fileName, contentType);
        try {
            Response response = documentUploadController.upload(request);
            return (UploadDocumentResponse) response.getEntity();
        } finally {
            Files.deleteIfExists(path);
        }
    }

    Response uploadResponse(UUID userId, String fileName, String contentType, byte[] bytes) throws IOException {
        UploadDocumentRequest request = new UploadDocumentRequest();
        request.userId = userId.toString();
        Path path = Files.createTempFile("integration-upload-", suffix(fileName));
        Files.write(path, bytes);
        request.file = new StubFileUpload(path, fileName, contentType);
        try {
            return documentUploadController.upload(request);
        } finally {
            Files.deleteIfExists(path);
        }
    }

    ProcessDocumentOutput process(UUID documentId, byte[] bytes) {
        return processDocument.execute(new ProcessDocumentInput(documentId, bytes));
    }

    Response documents(UUID userId, boolean includeAll) {
        return documentLibraryResource.getDocuments(userId.toString(), includeAll);
    }

    Response adminProgress(UUID userId) {
        return documentLibraryResource.getAdminProgress(userId.toString());
    }

    Response chat(UUID userId, String question, Integer timeoutMs) {
        return chatController.query(new ChatQueryRequest(question, timeoutMs), userId.toString(), securityContext(userId));
    }

    Response knowledgeGraphs(UUID userId) {
        return knowledgeGraphResource.listGraphs(userId.toString(), 0, 10);
    }

    Response knowledgeGraph(UUID userId, String graphId) {
        return knowledgeGraphResource.getGraph(userId.toString(), graphId, 0, 100);
    }

    Response knowledgeGraphSearch(UUID userId, String query, String graphId) {
        return knowledgeGraphResource.searchGraph(userId.toString(), query, graphId, List.of(), List.of(), 0, 20);
    }

    Response knowledgeGraphStatistics(UUID userId, String graphId) {
        return knowledgeGraphResource.getStatistics(userId.toString(), graphId);
    }

    byte[] loadResource(String path) throws IOException {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(path)) {
            if (inputStream == null) {
                throw new IllegalArgumentException("Missing resource: " + path);
            }
            return inputStream.readAllBytes();
        }
    }

    byte[] createPdf(String text) throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA, 12);
                contentStream.newLineAtOffset(50, 700);
                contentStream.showText(text);
                contentStream.endText();
            }
            document.save(outputStream);
            return outputStream.toByteArray();
        }
    }

    void updateStatus(UUID documentId, DocumentStatus status) {
        documentRepository.updateStatus(documentId, status);
    }

    private SecurityContext securityContext(UUID userId) {
        return new SecurityContext() {
            @Override
            public Principal getUserPrincipal() {
                return userId::toString;
            }

            @Override
            public boolean isUserInRole(String role) {
                return false;
            }

            @Override
            public boolean isSecure() {
                return false;
            }

            @Override
            public String getAuthenticationScheme() {
                return "test";
            }
        };
    }

    private User user(UUID userId, UserRole role) {
        return new User(userId, role.name().toLowerCase(), role.name().toLowerCase() + "@example.com", role,
            Instant.parse("2026-03-14T08:00:00Z"), true);
    }

    private String suffix(String fileName) {
        return fileName.substring(fileName.lastIndexOf('.'));
    }

    static final class InMemoryDocumentRepository implements DocumentRepository {
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
            return new ProcessingStatistics(
                documents.size(),
                findByStatus(DocumentStatus.UPLOADED).size(),
                findByStatus(DocumentStatus.PROCESSING).size(),
                findByStatus(DocumentStatus.READY).size(),
                findByStatus(DocumentStatus.FAILED).size()
            );
        }

        @Override
        public List<FailedDocumentInfo> findFailedDocuments() {
            return findByStatus(DocumentStatus.FAILED).stream()
                .map(document -> new FailedDocumentInfo(
                    document.documentId().toString(),
                    document.fileName(),
                    document.uploadedBy(),
                    document.uploadedAt(),
                    "Processing failed",
                    document.fileSize()))
                .toList();
        }

        @Override
        public List<ProcessingDocumentInfo> findProcessingDocuments() {
            return findByStatus(DocumentStatus.PROCESSING).stream()
                .map(document -> new ProcessingDocumentInfo(
                    document.documentId().toString(),
                    document.fileName(),
                    document.uploadedBy(),
                    document.uploadedAt(),
                    document.uploadedAt().plusSeconds(30)))
                .toList();
        }

        @Override
        public void updateStatus(UUID documentId, DocumentStatus status) {
            findById(documentId).ifPresent(document -> documents.put(documentId, document.withStatus(status)));
        }
    }

    static final class InMemoryUserRepository implements UserRepository {
        private final Map<UUID, User> users = new ConcurrentHashMap<>();

        @Override
        public User save(User user) {
            users.put(user.userId(), user);
            return user;
        }

        @Override
        public Optional<User> findById(UUID userId) {
            return Optional.ofNullable(users.get(userId));
        }

        @Override
        public Optional<User> findByUsername(String username) {
            return users.values().stream().filter(user -> user.username().equals(username)).findFirst();
        }

        @Override
        public Optional<User> findByEmail(String email) {
            return users.values().stream().filter(user -> user.email().equals(email)).findFirst();
        }

        @Override
        public boolean isAdmin(UUID userId) {
            return findById(userId).map(user -> user.role() == UserRole.ADMIN).orElse(false);
        }

        @Override
        public boolean isActiveUser(UUID userId) {
            return findById(userId).map(User::isActive).orElse(false);
        }

        void store(User user) {
            users.put(user.userId(), user);
        }

        List<User> allUsers() {
            return users.values().stream().toList();
        }
    }

    final class StubUserManagementFacade implements UserManagementFacade {
        @Override
        public AuthenticationResult authenticateUser(AuthenticationRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void invalidateSession(String sessionToken) {
        }

        @Override
        public boolean isAuthorized(UserId userId, String resource, String action) {
            return getUserRole(userId) == com.rag.app.user.domain.valueobjects.UserRole.ADMIN;
        }

        @Override
        public com.rag.app.user.domain.valueobjects.UserRole getUserRole(UserId userId) {
            return findUserById(userId)
                .map(com.rag.app.user.domain.entities.User::role)
                .orElse(com.rag.app.user.domain.valueobjects.UserRole.STANDARD);
        }

        @Override
        public GetUserProfileOutput getUserProfile(GetUserProfileInput input) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<com.rag.app.user.domain.entities.User> findUserById(UserId userId) {
            return userRepository.findById(userId.value())
                .map(user -> new com.rag.app.user.domain.entities.User(
                    new UserId(user.userId()),
                    user.username(),
                    user.email(),
                    user.role() == UserRole.ADMIN ? com.rag.app.user.domain.valueobjects.UserRole.ADMIN : com.rag.app.user.domain.valueobjects.UserRole.STANDARD,
                    user.createdAt(),
                    user.isActive()
                ));
        }

        @Override
        public boolean isActiveUser(UserId userId) {
            return findUserById(userId).map(com.rag.app.user.domain.entities.User::isActive).orElse(false);
        }

        @Override
        public ManageUserRolesOutput manageUserRoles(ManageUserRolesInput input) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<com.rag.app.user.domain.entities.User> getAllUsers() {
            return userRepository.allUsers().stream()
                .map(user -> new com.rag.app.user.domain.entities.User(
                    new UserId(user.userId()),
                    user.username(),
                    user.email(),
                    user.role() == UserRole.ADMIN ? com.rag.app.user.domain.valueobjects.UserRole.ADMIN : com.rag.app.user.domain.valueobjects.UserRole.STANDARD,
                    user.createdAt(),
                    user.isActive()
                ))
                .toList();
        }
    }

    static final class InMemoryKnowledgeGraphRepository implements KnowledgeGraphRepository {
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
            return graphs.values().stream()
                .flatMap(graph -> graph.relationshipsFor(nodeId).stream())
                .flatMap(relationship -> List.of(relationship.fromNodeId(), relationship.toNodeId()).stream())
                .filter(connectedNodeId -> !connectedNodeId.equals(nodeId))
                .map(this::findNode)
                .flatMap(Optional::stream)
                .distinct()
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
            return graphs.values().stream().filter(graph -> graph.containsNode(nodeId)).findFirst().orElseThrow();
        }

        private Optional<KnowledgeNode> findNode(NodeId nodeId) {
            return graphs.values().stream()
                .map(graph -> graph.getNode(nodeId))
                .flatMap(Optional::stream)
                .findFirst();
        }
    }

    private static final class StubFileUpload implements FileUpload {
        private final Path path;
        private final String fileName;
        private final String contentType;

        private StubFileUpload(Path path, String fileName, String contentType) {
            this.path = path;
            this.fileName = fileName;
            this.contentType = contentType;
        }

        @Override
        public String name() {
            return "file";
        }

        @Override
        public Path filePath() {
            return path;
        }

        @Override
        public String fileName() {
            return fileName;
        }

        @Override
        public long size() {
            try {
                return Files.size(path);
            } catch (IOException exception) {
                throw new IllegalStateException(exception);
            }
        }

        @Override
        public String contentType() {
            return contentType;
        }

        @Override
        public String charSet() {
            return StandardCharsets.UTF_8.name();
        }

        @Override
        public Path uploadedFile() {
            return path;
        }

        @Override
        public jakarta.ws.rs.core.MultivaluedMap<String, String> getHeaders() {
            return new jakarta.ws.rs.core.MultivaluedHashMap<>();
        }
    }
}
