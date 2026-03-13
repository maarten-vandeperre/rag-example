package com.rag.app.usecases;

import com.rag.app.domain.entities.Document;
import com.rag.app.domain.entities.User;
import com.rag.app.domain.valueobjects.DocumentMetadata;
import com.rag.app.domain.valueobjects.DocumentStatus;
import com.rag.app.domain.valueobjects.FileType;
import com.rag.app.domain.valueobjects.UserRole;
import com.rag.app.usecases.models.FailedDocumentInfo;
import com.rag.app.usecases.models.GetUserDocumentsInput;
import com.rag.app.usecases.models.GetUserDocumentsOutput;
import com.rag.app.usecases.models.ProcessingDocumentInfo;
import com.rag.app.usecases.models.ProcessingStatistics;
import com.rag.app.usecases.repositories.DocumentRepository;
import com.rag.app.usecases.repositories.UserRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GetUserDocumentsTest {

    @Test
    void shouldReturnOnlyStandardUserDocuments() {
        UUID standardUserId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        InMemoryUserRepository userRepository = new InMemoryUserRepository();
        userRepository.store(user(standardUserId, UserRole.STANDARD));
        userRepository.store(user(otherUserId, UserRole.STANDARD));

        InMemoryDocumentRepository documentRepository = new InMemoryDocumentRepository();
        documentRepository.store(document("new.txt", 2L, FileType.PLAIN_TEXT, DocumentStatus.READY,
            standardUserId, Instant.parse("2026-03-13T12:00:00Z")));
        documentRepository.store(document("old.pdf", 4L, FileType.PDF, DocumentStatus.UPLOADED,
            standardUserId, Instant.parse("2026-03-13T09:00:00Z")));
        documentRepository.store(document("other.md", 3L, FileType.MARKDOWN, DocumentStatus.FAILED,
            otherUserId, Instant.parse("2026-03-13T13:00:00Z")));

        GetUserDocuments useCase = new GetUserDocuments(documentRepository, userRepository);

        GetUserDocumentsOutput output = useCase.execute(new GetUserDocumentsInput(standardUserId, true));

        assertEquals(2, output.totalCount());
        assertEquals(List.of("new.txt", "old.pdf"), output.documents().stream().map(document -> document.fileName()).toList());
        assertEquals(List.of(DocumentStatus.READY, DocumentStatus.UPLOADED),
            output.documents().stream().map(document -> document.status()).toList());
    }

    @Test
    void shouldReturnAllDocumentsForAdminWhenRequested() {
        UUID adminUserId = UUID.randomUUID();
        UUID standardUserId = UUID.randomUUID();
        InMemoryUserRepository userRepository = new InMemoryUserRepository();
        userRepository.store(user(adminUserId, UserRole.ADMIN));
        userRepository.store(user(standardUserId, UserRole.STANDARD));

        InMemoryDocumentRepository documentRepository = new InMemoryDocumentRepository();
        documentRepository.store(document("admin.txt", 2L, FileType.PLAIN_TEXT, DocumentStatus.PROCESSING,
            adminUserId, Instant.parse("2026-03-13T09:00:00Z")));
        documentRepository.store(document("user.pdf", 4L, FileType.PDF, DocumentStatus.READY,
            standardUserId, Instant.parse("2026-03-13T10:00:00Z")));

        GetUserDocuments useCase = new GetUserDocuments(documentRepository, userRepository);

        GetUserDocumentsOutput output = useCase.execute(new GetUserDocumentsInput(adminUserId, true));

        assertEquals(2, output.totalCount());
        assertEquals(List.of("user.pdf", "admin.txt"), output.documents().stream().map(document -> document.fileName()).toList());
    }

    @Test
    void shouldReturnOnlyAdminDocumentsWhenNotIncludingAllDocuments() {
        UUID adminUserId = UUID.randomUUID();
        UUID standardUserId = UUID.randomUUID();
        InMemoryUserRepository userRepository = new InMemoryUserRepository();
        userRepository.store(user(adminUserId, UserRole.ADMIN));
        userRepository.store(user(standardUserId, UserRole.STANDARD));

        InMemoryDocumentRepository documentRepository = new InMemoryDocumentRepository();
        documentRepository.store(document("admin.txt", 2L, FileType.PLAIN_TEXT, DocumentStatus.PROCESSING,
            adminUserId, Instant.parse("2026-03-13T09:00:00Z")));
        documentRepository.store(document("user.pdf", 4L, FileType.PDF, DocumentStatus.READY,
            standardUserId, Instant.parse("2026-03-13T10:00:00Z")));

        GetUserDocuments useCase = new GetUserDocuments(documentRepository, userRepository);

        GetUserDocumentsOutput output = useCase.execute(new GetUserDocumentsInput(adminUserId, false));

        assertEquals(1, output.totalCount());
        assertEquals("admin.txt", output.documents().get(0).fileName());
        assertEquals(adminUserId.toString(), output.documents().get(0).uploadedBy());
        assertEquals(output.documents().get(0).uploadedAt(), output.documents().get(0).lastUpdated());
    }

    @Test
    void shouldRejectUnknownUser() {
        GetUserDocuments useCase = new GetUserDocuments(new InMemoryDocumentRepository(), new InMemoryUserRepository());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> useCase.execute(new GetUserDocumentsInput(UUID.randomUUID(), false)));

        assertEquals("userId user must exist", exception.getMessage());
    }

    private static User user(UUID userId, UserRole role) {
        return new User(userId, role.name().toLowerCase(), role.name().toLowerCase() + "@example.com", role,
            Instant.parse("2026-03-13T08:00:00Z"), true);
    }

    private static Document document(String fileName,
                                     long fileSize,
                                     FileType fileType,
                                     DocumentStatus status,
                                     UUID userId,
                                     Instant uploadedAt) {
        return new Document(UUID.randomUUID(), new DocumentMetadata(fileName, fileSize, fileType, fileName + "-hash"),
            userId.toString(), uploadedAt, status);
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

        void store(Document document) {
            documents.put(document.documentId(), document);
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
