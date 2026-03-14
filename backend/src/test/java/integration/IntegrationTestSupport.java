package integration;

import com.rag.app.api.ChatController;
import com.rag.app.api.DocumentLibraryResource;
import com.rag.app.api.DocumentUploadController;
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
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
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
import java.util.concurrent.Executor;

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

    IntegrationTestSupport() {
        Clock clock = Clock.fixed(Instant.parse("2026-03-14T10:00:00Z"), ZoneOffset.UTC);
        userRepository.store(user(STANDARD_USER_ID, UserRole.STANDARD));
        userRepository.store(user(OTHER_USER_ID, UserRole.STANDARD));
        userRepository.store(user(ADMIN_USER_ID, UserRole.ADMIN));

        UploadDocument uploadDocument = new UploadDocument(documentRepository, userRepository, clock);
        VectorStoreImpl vectorStore = new VectorStoreImpl(documentRepository, new TextChunker(), new EmbeddingGenerator());
        this.processDocument = new ProcessDocument(documentRepository, new DocumentContentExtractorImpl(), vectorStore);
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
        return chatController.query(new ChatQueryRequest(question, timeoutMs), securityContext(userId));
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
    }
}
