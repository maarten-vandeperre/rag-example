package com.rag.app.usecases;

import com.rag.app.domain.entities.Document;
import com.rag.app.domain.entities.User;
import com.rag.app.domain.valueobjects.DocumentStatus;
import com.rag.app.domain.valueobjects.FileType;
import com.rag.app.domain.valueobjects.UserRole;
import com.rag.app.usecases.models.FailedDocumentInfo;
import com.rag.app.usecases.models.ProcessDocumentInput;
import com.rag.app.usecases.models.ProcessDocumentOutput;
import com.rag.app.usecases.models.ProcessingDocumentInfo;
import com.rag.app.usecases.models.ProcessingStatistics;
import com.rag.app.usecases.models.UploadDocumentInput;
import com.rag.app.usecases.models.UploadDocumentOutput;
import com.rag.app.usecases.interfaces.DocumentContentExtractor;
import com.rag.app.usecases.interfaces.VectorStore;
import com.rag.app.usecases.repositories.DocumentRepository;
import com.rag.app.usecases.repositories.UserRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UploadDocumentTest {

    private static ProcessDocument createMockProcessDocument(DocumentRepository documentRepository) {
        DocumentContentExtractor mockExtractor = (content, fileType) -> "extracted text";
        VectorStore mockVectorStore = (documentId, content) -> {};
        return new ProcessDocument(documentRepository, mockExtractor, mockVectorStore);
    }

    @Test
    void shouldUploadDocumentWhenInputIsValid() {
        InMemoryDocumentRepository documentRepository = new InMemoryDocumentRepository();
        InMemoryUserRepository userRepository = new InMemoryUserRepository();
        UUID userId = UUID.randomUUID();
        userRepository.store(new User(userId, "jane", "jane@example.com", UserRole.STANDARD,
            Instant.parse("2026-03-13T10:00:00Z"), true));
        UploadDocument uploadDocument = new UploadDocument(
            documentRepository,
            userRepository,
            createMockProcessDocument(documentRepository),
            Clock.fixed(Instant.parse("2026-03-13T14:00:00Z"), ZoneOffset.UTC)
        );

        UploadDocumentOutput output = uploadDocument.execute(new UploadDocumentInput(
            "guide.pdf",
            4L,
            FileType.PDF,
            new byte[]{1, 2, 3, 4},
            userId
        ));

        assertNotNull(output.documentId());
        assertEquals(DocumentStatus.PROCESSING, output.status());
        assertEquals("Document uploaded and processing started", output.message());
        assertEquals(1, documentRepository.documents.size());
    }

    @Test
    void shouldRejectOversizedFiles() {
        InMemoryDocumentRepository documentRepository = new InMemoryDocumentRepository();
        UploadDocument uploadDocument = new UploadDocument(documentRepository, new InMemoryUserRepository(), createMockProcessDocument(documentRepository), Clock.systemUTC());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> uploadDocument.execute(
            new UploadDocumentInput("guide.pdf", 41_943_041L, FileType.PDF, new byte[]{1}, UUID.randomUUID())
        ));

        assertEquals("fileSize must be positive and less than or equal to 41943040", exception.getMessage());
    }

    @Test
    void shouldRejectMissingFileType() {
        InMemoryDocumentRepository documentRepository = new InMemoryDocumentRepository();
        UploadDocument uploadDocument = new UploadDocument(documentRepository, new InMemoryUserRepository(), createMockProcessDocument(documentRepository), Clock.systemUTC());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> uploadDocument.execute(
            new UploadDocumentInput("guide.pdf", 1L, null, new byte[]{1}, UUID.randomUUID())
        ));

        assertEquals("fileType must not be null", exception.getMessage());
    }

    @Test
    void shouldRejectUnknownUsers() {
        InMemoryDocumentRepository documentRepository = new InMemoryDocumentRepository();
        UploadDocument uploadDocument = new UploadDocument(documentRepository, new InMemoryUserRepository(), createMockProcessDocument(documentRepository), Clock.systemUTC());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> uploadDocument.execute(
            new UploadDocumentInput("guide.md", 1L, FileType.MARKDOWN, new byte[]{1}, UUID.randomUUID())
        ));

        assertEquals("uploadedBy user must exist", exception.getMessage());
    }

    @Test
    void shouldRejectInactiveUsers() {
        InMemoryUserRepository userRepository = new InMemoryUserRepository();
        UUID userId = UUID.randomUUID();
        userRepository.store(new User(userId, "jane", "jane@example.com", UserRole.STANDARD,
            Instant.parse("2026-03-13T10:00:00Z"), false));
        InMemoryDocumentRepository documentRepository = new InMemoryDocumentRepository();
        UploadDocument uploadDocument = new UploadDocument(documentRepository, userRepository, createMockProcessDocument(documentRepository), Clock.systemUTC());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> uploadDocument.execute(
            new UploadDocumentInput("guide.txt", 1L, FileType.PLAIN_TEXT, new byte[]{1}, userId)
        ));

        assertEquals("uploadedBy user must be active", exception.getMessage());
    }

    @Test
    void shouldRejectDuplicateContent() {
        InMemoryDocumentRepository documentRepository = new InMemoryDocumentRepository();
        InMemoryUserRepository userRepository = new InMemoryUserRepository();
        UUID userId = UUID.randomUUID();
        userRepository.store(new User(userId, "jane", "jane@example.com", UserRole.STANDARD,
            Instant.parse("2026-03-13T10:00:00Z"), true));
        UploadDocument uploadDocument = new UploadDocument(uploadRepository(documentRepository), userRepository, createMockProcessDocument(documentRepository), Clock.systemUTC());

        UploadDocumentInput input = new UploadDocumentInput("guide.pdf", 3L, FileType.PDF, new byte[]{9, 9, 9}, userId);
        uploadDocument.execute(input);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> uploadDocument.execute(input));

        assertEquals("document with identical content already exists", exception.getMessage());
    }

    private static InMemoryDocumentRepository uploadRepository(InMemoryDocumentRepository documentRepository) {
        return documentRepository;
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

    private static final class InMemoryUserRepository implements UserRepository {
        private final Map<UUID, User> users = new ConcurrentHashMap<>();

        @Override
        public Optional<User> findById(UUID userId) {
            return Optional.ofNullable(users.get(userId));
        }

        @Override
        public User save(User user) {
            users.put(user.userId(), user);
            return user;
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
}
