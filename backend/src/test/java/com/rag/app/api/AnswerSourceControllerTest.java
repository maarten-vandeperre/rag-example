package com.rag.app.api;

import com.rag.app.api.dto.AnswerSourceDetailsResponse;
import com.rag.app.domain.entities.ChatMessage;
import com.rag.app.domain.entities.Document;
import com.rag.app.domain.entities.User;
import com.rag.app.domain.valueobjects.AnswerSourceReference;
import com.rag.app.domain.valueobjects.DocumentMetadata;
import com.rag.app.domain.valueobjects.DocumentReference;
import com.rag.app.domain.valueobjects.DocumentStatus;
import com.rag.app.domain.valueobjects.FileType;
import com.rag.app.domain.valueobjects.UserRole;
import com.rag.app.usecases.interfaces.AnswerSourceChunkStore;
import com.rag.app.usecases.GetAnswerSourceDetails;
import com.rag.app.usecases.interfaces.DocumentChunkStore;
import com.rag.app.usecases.models.DocumentChunk;
import com.rag.app.usecases.repositories.AnswerSourceReferenceRepository;
import com.rag.app.usecases.repositories.ChatMessageRepository;
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

class AnswerSourceControllerTest {

    @Test
    void shouldReturnSourceDetailsForAccessibleAnswer() {
        UUID userId = UUID.randomUUID();
        UUID answerId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();

        InMemoryUserRepository userRepository = new InMemoryUserRepository();
        userRepository.store(user(userId));

        InMemoryDocumentRepository documentRepository = new InMemoryDocumentRepository();
        documentRepository.store(document(documentId, userId));

        InMemoryChatMessageRepository chatMessageRepository = new InMemoryChatMessageRepository();
        chatMessageRepository.store(new ChatMessage(
            answerId,
            userId,
            "Question",
            "Answer",
            List.of(new DocumentReference(documentId, "guide.pdf", "chunk-1", 0.97d)),
            Instant.parse("2026-03-16T10:00:00Z"),
            18L
        ));

        AnswerSourceController controller = new AnswerSourceController(new GetAnswerSourceDetails(
            chatMessageRepository,
            documentRepository,
            userRepository,
            new EmptyAnswerSourceReferenceRepository(),
            new EmptyAnswerSourceChunkStore(),
            documentId1 -> List.of(new DocumentChunk(documentId, "guide.pdf", "chunk-1", "Indexed content", 0.97d))
        ));

        Response response = controller.getAnswerSources(answerId.toString(), userId.toString(), securityContext(userId));

        assertEquals(200, response.getStatus());
        AnswerSourceDetailsResponse entity = assertInstanceOf(AnswerSourceDetailsResponse.class, response.getEntity());
        assertEquals(answerId.toString(), entity.answerId());
        assertEquals(1, entity.availableSources());
        assertEquals("guide.pdf", entity.sources().get(0).fileName());
        assertEquals("Indexed content", entity.sources().get(0).snippet().content());
    }

    @Test
    void shouldReturnNotFoundForUnknownAnswer() {
        UUID userId = UUID.randomUUID();
        InMemoryUserRepository userRepository = new InMemoryUserRepository();
        userRepository.store(user(userId));

        AnswerSourceController controller = new AnswerSourceController(new GetAnswerSourceDetails(
            new InMemoryChatMessageRepository(),
            new InMemoryDocumentRepository(),
            userRepository,
            new EmptyAnswerSourceReferenceRepository(),
            new EmptyAnswerSourceChunkStore(),
            documentId -> List.of()
        ));

        Response response = controller.getAnswerSources(UUID.randomUUID().toString(), userId.toString(), securityContext(userId));

        assertEquals(404, response.getStatus());
        AnswerSourceController.ErrorResponse entity = assertInstanceOf(AnswerSourceController.ErrorResponse.class, response.getEntity());
        assertEquals("answer not found", entity.message());
    }

    @Test
    void shouldReturnUnauthorizedWhenUserIsMissing() {
        AnswerSourceController controller = new AnswerSourceController(new GetAnswerSourceDetails(
            new InMemoryChatMessageRepository(),
            new InMemoryDocumentRepository(),
            new InMemoryUserRepository(),
            new EmptyAnswerSourceReferenceRepository(),
            new EmptyAnswerSourceChunkStore(),
            documentId -> List.of()
        ));

        Response response = controller.getAnswerSources(UUID.randomUUID().toString(), null, null);

        assertEquals(401, response.getStatus());
        AnswerSourceController.ErrorResponse entity = assertInstanceOf(AnswerSourceController.ErrorResponse.class, response.getEntity());
        assertEquals("authenticated user is required", entity.message());
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

    private static final class InMemoryChatMessageRepository implements ChatMessageRepository {
        private final Map<UUID, ChatMessage> messages = new ConcurrentHashMap<>();

        @Override
        public ChatMessage save(ChatMessage message) {
            messages.put(message.messageId(), message);
            return message;
        }

        @Override
        public Optional<ChatMessage> findById(UUID messageId) {
            return Optional.ofNullable(messages.get(messageId));
        }

        @Override
        public List<ChatMessage> findByUserId(UUID userId) {
            return messages.values().stream().filter(message -> message.userId().equals(userId)).toList();
        }

        @Override
        public List<ChatMessage> findRecentByUserId(UUID userId, int limit) {
            return findByUserId(userId).stream().limit(limit).toList();
        }

        void store(ChatMessage message) {
            messages.put(message.messageId(), message);
        }
    }

    private static final class EmptyAnswerSourceChunkStore implements AnswerSourceChunkStore {
        @Override
        public void store(UUID answerId, List<DocumentChunk> chunks) {
        }

        @Override
        public List<DocumentChunk> getChunks(UUID answerId) {
            return List.of();
        }
    }

    private static final class EmptyAnswerSourceReferenceRepository implements AnswerSourceReferenceRepository {
        @Override
        public void replaceForAnswer(UUID answerId, List<AnswerSourceReference> references) {
        }

        @Override
        public List<AnswerSourceReference> findByAnswerId(UUID answerId) {
            return List.of();
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
