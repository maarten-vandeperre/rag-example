package com.rag.app.api;

import com.rag.app.api.dto.AdminProgressResponse;
import com.rag.app.api.dto.DocumentListResponse;
import com.rag.app.domain.entities.Document;
import com.rag.app.domain.entities.User;
import com.rag.app.domain.valueobjects.DocumentMetadata;
import com.rag.app.domain.valueobjects.DocumentStatus;
import com.rag.app.domain.valueobjects.FileType;
import com.rag.app.domain.valueobjects.UserRole;
import com.rag.app.usecases.GetAdminProgress;
import com.rag.app.usecases.GetUserDocuments;
import com.rag.app.usecases.repositories.DocumentRepository;
import com.rag.app.usecases.repositories.UserRepository;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.Response;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class DocumentLibraryResourceTest {

    @Test
    void shouldReturnOnlyUserDocumentsForStandardUser() {
        UUID userId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        InMemoryUserRepository userRepository = new InMemoryUserRepository();
        userRepository.store(user(userId, UserRole.STANDARD));
        userRepository.store(user(otherUserId, UserRole.STANDARD));

        InMemoryDocumentRepository documentRepository = new InMemoryDocumentRepository();
        documentRepository.store(document("guide.pdf", userId, DocumentStatus.READY, Instant.parse("2026-03-13T10:00:00Z")));
        documentRepository.store(document("other.pdf", otherUserId, DocumentStatus.UPLOADED, Instant.parse("2026-03-13T11:00:00Z")));

        DocumentLibraryResource resource = new DocumentLibraryResource(
            new GetUserDocuments(documentRepository, userRepository),
            new GetAdminProgress(documentRepository, userRepository)
        );

        Response response = resource.getDocuments(userId.toString(), false);

        assertEquals(200, response.getStatus());
        DocumentListResponse entity = assertInstanceOf(DocumentListResponse.class, response.getEntity());
        assertEquals(1, entity.totalCount());
        assertEquals("guide.pdf", entity.documents().get(0).fileName());
    }

    @Test
    void shouldReturnAllDocumentsForAdminWhenIncludeAllIsTrue() {
        UUID adminId = UUID.randomUUID();
        UUID standardUserId = UUID.randomUUID();
        InMemoryUserRepository userRepository = new InMemoryUserRepository();
        userRepository.store(user(adminId, UserRole.ADMIN));
        userRepository.store(user(standardUserId, UserRole.STANDARD));

        InMemoryDocumentRepository documentRepository = new InMemoryDocumentRepository();
        documentRepository.store(document("admin.pdf", adminId, DocumentStatus.READY, Instant.parse("2026-03-13T09:00:00Z")));
        documentRepository.store(document("user.pdf", standardUserId, DocumentStatus.PROCESSING, Instant.parse("2026-03-13T10:00:00Z")));

        DocumentLibraryResource resource = new DocumentLibraryResource(
            new GetUserDocuments(documentRepository, userRepository),
            new GetAdminProgress(documentRepository, userRepository)
        );

        Response response = resource.getDocuments(adminId.toString(), true);

        assertEquals(200, response.getStatus());
        DocumentListResponse entity = assertInstanceOf(DocumentListResponse.class, response.getEntity());
        assertEquals(2, entity.totalCount());
        assertEquals(List.of("user.pdf", "admin.pdf"), entity.documents().stream().map(document -> document.fileName()).toList());
    }

    @Test
    void shouldReturnAdminProgressForAdminUsers() {
        UUID adminId = UUID.randomUUID();
        InMemoryUserRepository userRepository = new InMemoryUserRepository();
        userRepository.store(user(adminId, UserRole.ADMIN));

        InMemoryDocumentRepository documentRepository = new InMemoryDocumentRepository();
        documentRepository.store(document("failed.pdf", UUID.randomUUID(), DocumentStatus.FAILED, Instant.parse("2026-03-13T09:00:00Z")));
        documentRepository.store(document("processing.pdf", UUID.randomUUID(), DocumentStatus.PROCESSING, Instant.parse("2026-03-13T10:00:00Z")));

        DocumentLibraryResource resource = new DocumentLibraryResource(
            new GetUserDocuments(documentRepository, userRepository),
            new GetAdminProgress(documentRepository, userRepository)
        );

        Response response = resource.getAdminProgress(adminId.toString());

        assertEquals(200, response.getStatus());
        AdminProgressResponse entity = assertInstanceOf(AdminProgressResponse.class, response.getEntity());
        assertEquals(2, entity.statistics().totalDocuments());
        assertEquals(1, entity.failedDocuments().size());
        assertEquals(1, entity.processingDocuments().size());
    }

    @Test
    void shouldReturnForbiddenForStandardUsersOnAdminProgress() {
        UUID userId = UUID.randomUUID();
        InMemoryUserRepository userRepository = new InMemoryUserRepository();
        userRepository.store(user(userId, UserRole.STANDARD));

        DocumentLibraryResource resource = new DocumentLibraryResource(
            new GetUserDocuments(new InMemoryDocumentRepository(), userRepository),
            new GetAdminProgress(new InMemoryDocumentRepository(), userRepository)
        );

        Response response = resource.getAdminProgress(userId.toString());

        assertEquals(403, response.getStatus());
        DocumentLibraryResource.ErrorResponse entity = assertInstanceOf(DocumentLibraryResource.ErrorResponse.class, response.getEntity());
        assertEquals("adminUserId user must be an admin", entity.message());
    }

    private static User user(UUID userId, UserRole role) {
        return new User(userId, role.name().toLowerCase(), role.name().toLowerCase() + "@example.com", role,
            Instant.parse("2026-03-13T08:00:00Z"), true);
    }

    private static Document document(String fileName, UUID userId, DocumentStatus status, Instant uploadedAt) {
        return new Document(UUID.randomUUID(), new DocumentMetadata(fileName, 128L, FileType.PDF, fileName + "-hash"),
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
            return documents.values().stream().filter(document -> document.status() == status).toList();
        }

        @Override
        public com.rag.app.usecases.models.ProcessingStatistics getProcessingStatistics() {
            int uploaded = findByStatus(DocumentStatus.UPLOADED).size();
            int processing = findByStatus(DocumentStatus.PROCESSING).size();
            int ready = findByStatus(DocumentStatus.READY).size();
            int failed = findByStatus(DocumentStatus.FAILED).size();
            return new com.rag.app.usecases.models.ProcessingStatistics(documents.size(), uploaded, processing, ready, failed);
        }

        @Override
        public List<com.rag.app.usecases.models.FailedDocumentInfo> findFailedDocuments() {
            return findByStatus(DocumentStatus.FAILED).stream()
                .map(document -> new com.rag.app.usecases.models.FailedDocumentInfo(
                    document.documentId().toString(),
                    document.fileName(),
                    document.uploadedBy(),
                    document.uploadedAt(),
                    "Processing failed",
                    document.fileSize()
                ))
                .toList();
        }

        @Override
        public List<com.rag.app.usecases.models.ProcessingDocumentInfo> findProcessingDocuments() {
            return findByStatus(DocumentStatus.PROCESSING).stream()
                .map(document -> new com.rag.app.usecases.models.ProcessingDocumentInfo(
                    document.documentId().toString(),
                    document.fileName(),
                    document.uploadedBy(),
                    document.uploadedAt(),
                    document.uploadedAt().plusSeconds(60)
                ))
                .toList();
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
}
