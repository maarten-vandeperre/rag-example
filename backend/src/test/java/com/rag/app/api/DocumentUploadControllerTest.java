package com.rag.app.api;

import com.rag.app.api.dto.ErrorResponse;
import com.rag.app.api.dto.UploadDocumentRequest;
import com.rag.app.api.dto.UploadDocumentResponse;
import com.rag.app.domain.entities.Document;
import com.rag.app.domain.entities.User;
import com.rag.app.domain.valueobjects.DocumentStatus;
import com.rag.app.domain.valueobjects.UserRole;
import com.rag.app.usecases.UploadDocument;
import com.rag.app.usecases.models.FailedDocumentInfo;
import com.rag.app.usecases.models.ProcessingDocumentInfo;
import com.rag.app.usecases.models.ProcessingStatistics;
import com.rag.app.usecases.repositories.DocumentRepository;
import com.rag.app.usecases.repositories.UserRepository;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.core.Response;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DocumentUploadControllerTest {

    @Test
    void shouldUploadValidDocument() throws Exception {
        UUID userId = UUID.randomUUID();
        InMemoryUserRepository userRepository = new InMemoryUserRepository();
        userRepository.save(new User(userId, "uploader", "uploader@example.com", UserRole.STANDARD,
            Instant.parse("2026-03-13T08:00:00Z"), true));
        DocumentUploadController controller = new DocumentUploadController(
            new UploadDocument(new InMemoryDocumentRepository(), userRepository, Clock.fixed(Instant.parse("2026-03-13T10:30:00Z"), ZoneOffset.UTC)),
            Clock.fixed(Instant.parse("2026-03-13T10:30:00Z"), ZoneOffset.UTC)
        );
        UploadDocumentRequest request = new UploadDocumentRequest();
        request.userId = userId.toString();
        request.file = new StubFileUpload(writeTempFile("guide.pdf", "pdf".getBytes()), "guide.pdf", "application/pdf");

        Response response = controller.upload(request);

        assertEquals(201, response.getStatus());
        UploadDocumentResponse body = assertInstanceOf(UploadDocumentResponse.class, response.getEntity());
        assertNotNull(body.documentId());
        assertEquals("guide.pdf", body.fileName());
        assertEquals("UPLOADED", body.status());
        assertEquals("Document uploaded successfully", body.message());
        assertEquals(Instant.parse("2026-03-13T10:30:00Z"), body.uploadedAt());
    }

    @Test
    void shouldRejectOversizedFiles() throws Exception {
        DocumentUploadController controller = controllerWithActiveUser();
        Path tempFile = Files.createTempFile("large-upload", ".pdf");
        try {
            try (RandomAccessFile file = new RandomAccessFile(tempFile.toFile(), "rw")) {
                file.setLength(41_943_041L);
            }
            UploadDocumentRequest request = new UploadDocumentRequest();
            request.userId = ACTIVE_USER_ID.toString();
            request.file = new StubFileUpload(tempFile, "large.pdf", "application/pdf");

            Response response = controller.upload(request);

            assertEquals(413, response.getStatus());
            ErrorResponse body = assertInstanceOf(ErrorResponse.class, response.getEntity());
            assertEquals("FILE_TOO_LARGE", body.error());
            assertEquals("File size exceeds maximum allowed size of 40MB", body.message());
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    void shouldRejectUnsupportedFileTypes() throws Exception {
        DocumentUploadController controller = controllerWithActiveUser();
        UploadDocumentRequest request = new UploadDocumentRequest();
        request.userId = ACTIVE_USER_ID.toString();
        request.file = new StubFileUpload(writeTempFile("guide.docx", "docx".getBytes()), "guide.docx",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

        Response response = controller.upload(request);

        assertEquals(415, response.getStatus());
        ErrorResponse body = assertInstanceOf(ErrorResponse.class, response.getEntity());
        assertEquals("UNSUPPORTED_FILE_TYPE", body.error());
        assertEquals("Only PDF, Markdown, and plain text files are supported", body.message());
    }

    @Test
    void shouldRejectMalformedRequests() {
        DocumentUploadController controller = controllerWithActiveUser();
        UploadDocumentRequest request = new UploadDocumentRequest();
        request.userId = "not-a-uuid";

        Response response = controller.upload(request);

        assertEquals(400, response.getStatus());
        ErrorResponse body = assertInstanceOf(ErrorResponse.class, response.getEntity());
        assertEquals("INVALID_REQUEST", body.error());
        assertEquals("File and userId are required", body.message());
    }

    private static final UUID ACTIVE_USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static DocumentUploadController controllerWithActiveUser() {
        InMemoryUserRepository userRepository = new InMemoryUserRepository();
        userRepository.save(new User(ACTIVE_USER_ID, "uploader", "uploader@example.com", UserRole.STANDARD,
            Instant.parse("2026-03-13T08:00:00Z"), true));
        Clock fixedClock = Clock.fixed(Instant.parse("2026-03-13T10:30:00Z"), ZoneOffset.UTC);
        return new DocumentUploadController(new UploadDocument(new InMemoryDocumentRepository(), userRepository, fixedClock), fixedClock);
    }

    private static Path writeTempFile(String fileName, byte[] content) throws Exception {
        String suffix = fileName.substring(fileName.lastIndexOf('.'));
        Path path = Files.createTempFile("upload-", suffix);
        Files.write(path, content);
        return path;
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
            } catch (Exception exception) {
                throw new IllegalStateException(exception);
            }
        }

        @Override
        public String contentType() {
            return contentType;
        }

        @Override
        public String charSet() {
            return null;
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

    private static final class InMemoryUserRepository implements UserRepository {
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
    }
}
