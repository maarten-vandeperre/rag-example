package com.rag.app.api;

import com.rag.app.api.dto.DocumentContentResponse;
import com.rag.app.domain.entities.Document;
import com.rag.app.domain.entities.User;
import com.rag.app.domain.valueobjects.DocumentMetadata;
import com.rag.app.domain.valueobjects.DocumentStatus;
import com.rag.app.domain.valueobjects.FileType;
import com.rag.app.domain.valueobjects.UserRole;
import com.rag.app.usecases.GetDocumentContent;
import com.rag.app.usecases.models.DocumentChunk;
import com.rag.app.usecases.repositories.DocumentRepository;
import com.rag.app.usecases.repositories.UserRepository;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class DocumentContentControllerTest {

    @Test
    void shouldReturnDocumentContentForAccessibleDocument() {
        UUID userId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();

        InMemoryUserRepository userRepository = new InMemoryUserRepository();
        userRepository.store(user(userId));

        InMemoryDocumentRepository documentRepository = new InMemoryDocumentRepository();
        documentRepository.store(document(documentId, userId));

        DocumentContentController controller = new DocumentContentController(new GetDocumentContent(
            documentRepository,
            userRepository,
            requestedDocumentId -> List.of(
                new DocumentChunk(documentId, "guide.pdf", "chunk-1", "First paragraph", 0.91d),
                new DocumentChunk(documentId, "guide.pdf", "chunk-2", "Second paragraph", 0.82d)
            )
        ));

        Response response = controller.getDocumentContent(documentId.toString(), userId.toString(), securityContext(userId));

        assertEquals(200, response.getStatus());
        DocumentContentResponse entity = assertInstanceOf(DocumentContentResponse.class, response.getEntity());
        assertEquals(documentId.toString(), entity.documentId());
        assertEquals("guide.pdf", entity.fileName());
        assertEquals("First paragraph\n\nSecond paragraph", entity.content());
        assertEquals(2, entity.metadata().pageCount());
    }

    @Test
    void shouldReturnNotFoundForUnknownDocument() {
        UUID userId = UUID.randomUUID();
        InMemoryUserRepository userRepository = new InMemoryUserRepository();
        userRepository.store(user(userId));

        DocumentContentController controller = new DocumentContentController(new GetDocumentContent(
            new InMemoryDocumentRepository(),
            userRepository,
            documentId -> List.of()
        ));

        Response response = controller.getDocumentContent(UUID.randomUUID().toString(), userId.toString(), securityContext(userId));

        assertEquals(404, response.getStatus());
        AnswerSourceController.ErrorResponse entity = assertInstanceOf(AnswerSourceController.ErrorResponse.class, response.getEntity());
        assertEquals("document not found", entity.message());
    }

    private static User user(UUID userId) {
        return new User(userId, "user-" + userId, userId + "@example.com", UserRole.STANDARD, Instant.parse("2026-03-16T08:00:00Z"), true);
    }

    private static Document document(UUID documentId, UUID userId) {
        return new Document(documentId, new DocumentMetadata("guide.pdf", 128L, FileType.PDF, "hash-" + documentId),
            userId.toString(), Instant.parse("2026-03-16T08:15:00Z"), DocumentStatus.READY);
    }

    private static SecurityContext securityContext(UUID userId) {
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
        public com.rag.app.usecases.models.ProcessingStatistics getProcessingStatistics() {
            return new com.rag.app.usecases.models.ProcessingStatistics(documents.size(), 0, 0, 0, 0);
        }

        @Override
        public List<com.rag.app.usecases.models.FailedDocumentInfo> findFailedDocuments() {
            return List.of();
        }

        @Override
        public List<com.rag.app.usecases.models.ProcessingDocumentInfo> findProcessingDocuments() {
            return List.of();
        }

        @Override
        public void updateStatus(UUID documentId, DocumentStatus status) {
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
