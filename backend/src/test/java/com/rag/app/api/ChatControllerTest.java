package com.rag.app.api;

import com.rag.app.api.dto.ChatQueryRequest;
import com.rag.app.api.dto.ChatQueryResponse;
import com.rag.app.domain.entities.Document;
import com.rag.app.domain.entities.User;
import com.rag.app.domain.valueobjects.DocumentMetadata;
import com.rag.app.domain.valueobjects.DocumentReference;
import com.rag.app.domain.valueobjects.DocumentStatus;
import com.rag.app.domain.valueobjects.FileType;
import com.rag.app.domain.valueobjects.UserRole;
import com.rag.app.usecases.QueryDocuments;
import com.rag.app.usecases.interfaces.AnswerGenerator;
import com.rag.app.usecases.interfaces.SemanticSearch;
import com.rag.app.usecases.models.DocumentChunk;
import com.rag.app.usecases.models.FailedDocumentInfo;
import com.rag.app.usecases.models.ProcessingDocumentInfo;
import com.rag.app.usecases.models.ProcessingStatistics;
import com.rag.app.usecases.repositories.DocumentRepository;
import com.rag.app.usecases.repositories.UserRepository;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
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

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChatControllerTest {
    private static final Executor DAEMON_EXECUTOR = command -> {
        Thread thread = new Thread(command);
        thread.setDaemon(true);
        thread.start();
    };

    @Test
    void shouldReturnAnswerAndDocumentReferencesForValidQuery() {
        UUID userId = UUID.randomUUID();
        QueryDocuments queryDocuments = createUseCase(
            user(userId),
            List.of(readyDocument(userId)),
            (query, documentIds) -> List.of(new DocumentChunk(UUID.randomUUID(), "guide.pdf", "p-12", "context", 0.98d)),
            (question, context) -> new AnswerGenerator.GeneratedAnswer(
                "Upload the PDF and wait for processing.",
                List.of(new DocumentReference(context.get(0).documentId(), context.get(0).documentName(), context.get(0).paragraphReference(), context.get(0).relevanceScore()))
            ),
            Clock.fixed(Instant.parse("2026-03-13T18:00:00Z"), ZoneOffset.UTC)
        );
        ChatController controller = new ChatController(queryDocuments, DAEMON_EXECUTOR);

        Response response = controller.query(new ChatQueryRequest("How do uploads work?", null), userId.toString(), securityContext(userId));

        ChatQueryResponse entity = (ChatQueryResponse) response.getEntity();
        assertEquals(200, response.getStatus());
        assertEquals(true, entity.success());
        assertEquals("Upload the PDF and wait for processing.", entity.answer());
        assertEquals(1, entity.documentReferences().size());
        assertEquals("guide.pdf", entity.documentReferences().get(0).documentName());
        assertEquals(null, entity.errorMessage());
    }

    @Test
    void shouldReturnBadRequestForBlankQuestion() {
        UUID userId = UUID.randomUUID();
        ChatController controller = new ChatController(createUseCase(
            user(userId),
            List.of(readyDocument(userId)),
            (query, documentIds) -> List.of(),
            (question, context) -> new AnswerGenerator.GeneratedAnswer("unused", List.of()),
            Clock.systemUTC()
        ), DAEMON_EXECUTOR);

        Response response = controller.query(new ChatQueryRequest("   ", null), userId.toString(), securityContext(userId));

        ChatQueryResponse entity = (ChatQueryResponse) response.getEntity();
        assertEquals(400, response.getStatus());
        assertEquals(false, entity.success());
        assertEquals("question must not be null or empty", entity.errorMessage());
    }

    @Test
    void shouldReturnNotFoundWhenNoRelevantDocumentsAreFound() {
        UUID userId = UUID.randomUUID();
        ChatController controller = new ChatController(createUseCase(
            user(userId),
            List.of(readyDocument(userId)),
            (query, documentIds) -> List.of(),
            (question, context) -> new AnswerGenerator.GeneratedAnswer("unused", List.of()),
            Clock.fixed(Instant.parse("2026-03-13T18:00:00Z"), ZoneOffset.UTC)
        ), DAEMON_EXECUTOR);

        Response response = controller.query(new ChatQueryRequest("Unknown topic", null), userId.toString(), securityContext(userId));

        ChatQueryResponse entity = (ChatQueryResponse) response.getEntity();
        assertEquals(404, response.getStatus());
        assertEquals("no answer found", entity.errorMessage());
    }

    @Test
    void shouldReturnRequestTimeoutWhenQueryExecutionExceedsLimit() {
        UUID userId = UUID.randomUUID();
        QueryDocuments queryDocuments = createUseCase(
            user(userId),
            List.of(readyDocument(userId)),
            (query, documentIds) -> {
                sleep(50L);
                return List.of(new DocumentChunk(UUID.randomUUID(), "guide.pdf", "p-12", "context", 0.98d));
            },
            (question, context) -> new AnswerGenerator.GeneratedAnswer("Late answer", List.of()),
            Clock.systemUTC()
        );
        ChatController controller = new ChatController(queryDocuments, DAEMON_EXECUTOR);

        Response response = controller.query(new ChatQueryRequest("Slow question", 10), userId.toString(), securityContext(userId));

        ChatQueryResponse entity = (ChatQueryResponse) response.getEntity();
        assertEquals(408, response.getStatus());
        assertEquals("Query exceeded the allowed response time", entity.errorMessage());
    }

    @Test
    void shouldReturnInternalServerErrorWhenProcessingFails() {
        UUID userId = UUID.randomUUID();
        ChatController controller = new ChatController(createUseCase(
            user(userId),
            List.of(readyDocument(userId)),
            (query, documentIds) -> {
                throw new IllegalStateException("search backend unavailable");
            },
            (question, context) -> new AnswerGenerator.GeneratedAnswer("unused", List.of()),
            Clock.systemUTC()
        ), DAEMON_EXECUTOR);

        Response response = controller.query(new ChatQueryRequest("Will this fail?", null), userId.toString(), securityContext(userId));

        ChatQueryResponse entity = (ChatQueryResponse) response.getEntity();
        assertEquals(500, response.getStatus());
        assertEquals("Unable to process chat query", entity.errorMessage());
    }

    private static QueryDocuments createUseCase(User user,
                                                List<Document> documents,
                                                SemanticSearch semanticSearch,
                                                AnswerGenerator answerGenerator,
                                                Clock clock) {
        InMemoryUserRepository userRepository = new InMemoryUserRepository();
        userRepository.store(user);
        InMemoryDocumentRepository documentRepository = new InMemoryDocumentRepository();
        documents.forEach(documentRepository::store);
        return new QueryDocuments(userRepository, documentRepository, semanticSearch, answerGenerator, clock);
    }

    private static User user(UUID userId) {
        return new User(userId, "user", "user@example.com", UserRole.STANDARD,
            Instant.parse("2026-03-13T10:00:00Z"), true);
    }

    private static Document readyDocument(UUID userId) {
        return new Document(
            UUID.randomUUID(),
            new DocumentMetadata("guide.pdf", 123L, FileType.PDF, "hash-guide"),
            userId.toString(),
            Instant.parse("2026-03-13T12:00:00Z"),
            DocumentStatus.READY
        );
    }

    private static SecurityContext securityContext(UUID userId) {
        return new SecurityContext() {
            @Override
            public Principal getUserPrincipal() {
                return () -> userId.toString();
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

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("sleep interrupted", exception);
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
            Document existing = documents.get(documentId);
            if (existing != null) {
                documents.put(documentId, existing.withStatus(status));
            }
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
