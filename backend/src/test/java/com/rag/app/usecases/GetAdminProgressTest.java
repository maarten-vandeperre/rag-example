package com.rag.app.usecases;

import com.rag.app.domain.entities.Document;
import com.rag.app.domain.entities.User;
import com.rag.app.domain.valueobjects.DocumentMetadata;
import com.rag.app.domain.valueobjects.DocumentStatus;
import com.rag.app.domain.valueobjects.FileType;
import com.rag.app.domain.valueobjects.UserRole;
import com.rag.app.usecases.models.FailedDocumentInfo;
import com.rag.app.usecases.models.GetAdminProgressInput;
import com.rag.app.usecases.models.GetAdminProgressOutput;
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

class GetAdminProgressTest {

    @Test
    void shouldReturnProcessingStatisticsAndDocumentListsForAdmins() {
        UUID adminUserId = UUID.randomUUID();
        InMemoryUserRepository userRepository = new InMemoryUserRepository();
        userRepository.store(new User(adminUserId, "admin", "admin@example.com", UserRole.ADMIN,
            Instant.parse("2026-03-13T09:00:00Z"), true));

        FailedDocumentInfo olderFailedDocument = new FailedDocumentInfo(
            UUID.randomUUID().toString(),
            "older.pdf",
            "alice",
            Instant.parse("2026-03-13T08:00:00Z"),
            "Parser error",
            256L
        );
        FailedDocumentInfo newerFailedDocument = new FailedDocumentInfo(
            UUID.randomUUID().toString(),
            "newer.pdf",
            "bob",
            Instant.parse("2026-03-13T10:00:00Z"),
            "Timeout",
            512L
        );
        ProcessingDocumentInfo longerRunningDocument = new ProcessingDocumentInfo(
            UUID.randomUUID().toString(),
            "contract.pdf",
            "carol",
            Instant.parse("2026-03-13T07:00:00Z"),
            Instant.parse("2026-03-13T07:30:00Z")
        );
        ProcessingDocumentInfo recentProcessingDocument = new ProcessingDocumentInfo(
            UUID.randomUUID().toString(),
            "invoice.pdf",
            "dave",
            Instant.parse("2026-03-13T09:30:00Z"),
            Instant.parse("2026-03-13T09:45:00Z")
        );

        InMemoryDocumentRepository documentRepository = new InMemoryDocumentRepository(
            new ProcessingStatistics(5, 1, 2, 1, 1),
            List.of(olderFailedDocument, newerFailedDocument),
            List.of(recentProcessingDocument, longerRunningDocument)
        );

        GetAdminProgress useCase = new GetAdminProgress(documentRepository, userRepository);

        GetAdminProgressOutput output = useCase.execute(new GetAdminProgressInput(adminUserId.toString()));

        assertEquals(new ProcessingStatistics(5, 1, 2, 1, 1), output.processingStatistics());
        assertEquals(List.of(newerFailedDocument, olderFailedDocument), output.failedDocuments());
        assertEquals(List.of(longerRunningDocument, recentProcessingDocument), output.processingDocuments());
        assertEquals(Instant.parse("2026-03-13T07:30:00Z"), output.processingDocuments().get(0).processingStartedAt());
    }

    @Test
    void shouldRejectStandardUsers() {
        UUID userId = UUID.randomUUID();
        InMemoryUserRepository userRepository = new InMemoryUserRepository();
        userRepository.store(new User(userId, "jane", "jane@example.com", UserRole.STANDARD,
            Instant.parse("2026-03-13T09:00:00Z"), true));
        GetAdminProgress useCase = new GetAdminProgress(new InMemoryDocumentRepository(), userRepository);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> useCase.execute(new GetAdminProgressInput(userId.toString())));

        assertEquals("adminUserId user must be an admin", exception.getMessage());
    }

    @Test
    void shouldRejectUnknownAdminUsers() {
        GetAdminProgress useCase = new GetAdminProgress(new InMemoryDocumentRepository(), new InMemoryUserRepository());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> useCase.execute(new GetAdminProgressInput(UUID.randomUUID().toString())));

        assertEquals("adminUserId user must exist", exception.getMessage());
    }

    @Test
    void shouldRejectInvalidAdminUserIdentifiers() {
        GetAdminProgress useCase = new GetAdminProgress(new InMemoryDocumentRepository(), new InMemoryUserRepository());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> useCase.execute(new GetAdminProgressInput("not-a-uuid")));

        assertEquals("adminUserId must be a valid UUID", exception.getMessage());
    }

    private static final class InMemoryDocumentRepository implements DocumentRepository {
        private final Map<UUID, Document> documents = new ConcurrentHashMap<>();
        private final ProcessingStatistics processingStatistics;
        private final List<FailedDocumentInfo> failedDocuments;
        private final List<ProcessingDocumentInfo> processingDocuments;

        private InMemoryDocumentRepository() {
            this(new ProcessingStatistics(0, 0, 0, 0, 0), List.of(), List.of());
        }

        private InMemoryDocumentRepository(ProcessingStatistics processingStatistics,
                                           List<FailedDocumentInfo> failedDocuments,
                                           List<ProcessingDocumentInfo> processingDocuments) {
            this.processingStatistics = processingStatistics;
            this.failedDocuments = failedDocuments;
            this.processingDocuments = processingDocuments;
        }

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
