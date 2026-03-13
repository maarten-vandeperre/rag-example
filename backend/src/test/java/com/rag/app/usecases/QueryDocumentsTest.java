package com.rag.app.usecases;

import com.rag.app.domain.entities.Document;
import com.rag.app.domain.entities.User;
import com.rag.app.domain.valueobjects.DocumentMetadata;
import com.rag.app.domain.valueobjects.DocumentReference;
import com.rag.app.domain.valueobjects.DocumentStatus;
import com.rag.app.domain.valueobjects.FileType;
import com.rag.app.domain.valueobjects.UserRole;
import com.rag.app.usecases.interfaces.AnswerGenerator;
import com.rag.app.usecases.interfaces.SemanticSearch;
import com.rag.app.usecases.models.DocumentChunk;
import com.rag.app.usecases.models.FailedDocumentInfo;
import com.rag.app.usecases.models.ProcessingDocumentInfo;
import com.rag.app.usecases.models.ProcessingStatistics;
import com.rag.app.usecases.models.QueryDocumentsInput;
import com.rag.app.usecases.models.QueryDocumentsOutput;
import com.rag.app.usecases.repositories.DocumentRepository;
import com.rag.app.usecases.repositories.UserRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QueryDocumentsTest {

    @Test
    void shouldSearchOnlyReadyDocumentsOwnedByStandardUser() {
        InMemoryUserRepository userRepository = new InMemoryUserRepository();
        InMemoryDocumentRepository documentRepository = new InMemoryDocumentRepository();
        UUID userId = UUID.randomUUID();
        userRepository.store(user(userId, UserRole.STANDARD));
        documentRepository.store(document(UUID.randomUUID(), userId, DocumentStatus.READY, "owned-ready.pdf"));
        documentRepository.store(document(UUID.randomUUID(), userId, DocumentStatus.UPLOADED, "owned-uploaded.pdf"));
        documentRepository.store(document(UUID.randomUUID(), UUID.randomUUID(), DocumentStatus.READY, "other-ready.pdf"));
        RecordingSemanticSearch semanticSearch = new RecordingSemanticSearch(List.of(chunk("owned-ready.pdf")));
        QueryDocuments useCase = new QueryDocuments(
            userRepository,
            documentRepository,
            semanticSearch,
            (question, context) -> new AnswerGenerator.GeneratedAnswer("Answer", referencesFrom(context)),
            Clock.fixed(Instant.parse("2026-03-13T18:00:00Z"), ZoneOffset.UTC)
        );

        QueryDocumentsOutput output = useCase.execute(new QueryDocumentsInput(userId, "What is in my file?"));

        assertTrue(output.success());
        assertEquals(1, semanticSearch.documentIds.size());
        assertEquals("Answer", output.answer());
        assertEquals("owned-ready.pdf", output.documentReferences().get(0).documentName());
    }

    @Test
    void shouldSearchAllReadyDocumentsForAdminUser() {
        InMemoryUserRepository userRepository = new InMemoryUserRepository();
        InMemoryDocumentRepository documentRepository = new InMemoryDocumentRepository();
        UUID adminId = UUID.randomUUID();
        userRepository.store(user(adminId, UserRole.ADMIN));
        documentRepository.store(document(UUID.randomUUID(), UUID.randomUUID(), DocumentStatus.READY, "first.pdf"));
        documentRepository.store(document(UUID.randomUUID(), UUID.randomUUID(), DocumentStatus.READY, "second.pdf"));
        RecordingSemanticSearch semanticSearch = new RecordingSemanticSearch(List.of(chunk("first.pdf"), chunk("second.pdf")));
        QueryDocuments useCase = new QueryDocuments(
            userRepository,
            documentRepository,
            semanticSearch,
            (question, context) -> new AnswerGenerator.GeneratedAnswer("Admin answer", referencesFrom(context)),
            Clock.fixed(Instant.parse("2026-03-13T18:00:00Z"), ZoneOffset.UTC)
        );

        QueryDocumentsOutput output = useCase.execute(new QueryDocumentsInput(adminId, "Search all documents"));

        assertTrue(output.success());
        assertEquals(2, semanticSearch.documentIds.size());
    }

    @Test
    void shouldReturnNoMatchesMessageWhenSearchReturnsNothing() {
        InMemoryUserRepository userRepository = new InMemoryUserRepository();
        InMemoryDocumentRepository documentRepository = new InMemoryDocumentRepository();
        UUID userId = UUID.randomUUID();
        userRepository.store(user(userId, UserRole.STANDARD));
        documentRepository.store(document(UUID.randomUUID(), userId, DocumentStatus.READY, "owned-ready.pdf"));
        QueryDocuments useCase = new QueryDocuments(
            userRepository,
            documentRepository,
            (query, documentIds) -> List.of(),
            (question, context) -> new AnswerGenerator.GeneratedAnswer("unused", List.of()),
            Clock.fixed(Instant.parse("2026-03-13T18:00:00Z"), ZoneOffset.UTC)
        );

        QueryDocumentsOutput output = useCase.execute(new QueryDocumentsInput(userId, "Unknown topic"));

        assertFalse(output.success());
        assertEquals("No relevant documents found for the question", output.errorMessage());
    }

    @Test
    void shouldReturnNoDocumentsMessageWhenAccessibleReadyDocumentsAreMissing() {
        InMemoryUserRepository userRepository = new InMemoryUserRepository();
        UUID userId = UUID.randomUUID();
        userRepository.store(user(userId, UserRole.STANDARD));
        QueryDocuments useCase = new QueryDocuments(
            userRepository,
            new InMemoryDocumentRepository(),
            (query, documentIds) -> List.of(),
            (question, context) -> new AnswerGenerator.GeneratedAnswer("unused", List.of()),
            Clock.fixed(Instant.parse("2026-03-13T18:00:00Z"), ZoneOffset.UTC)
        );

        QueryDocumentsOutput output = useCase.execute(new QueryDocumentsInput(userId, "Any documents?"));

        assertFalse(output.success());
        assertEquals("No ready documents available for this query", output.errorMessage());
    }

    @Test
    void shouldTrackAndEnforceMaximumResponseTime() {
        InMemoryUserRepository userRepository = new InMemoryUserRepository();
        InMemoryDocumentRepository documentRepository = new InMemoryDocumentRepository();
        UUID userId = UUID.randomUUID();
        userRepository.store(user(userId, UserRole.STANDARD));
        documentRepository.store(document(UUID.randomUUID(), userId, DocumentStatus.READY, "owned-ready.pdf"));
        MutableClock clock = new MutableClock(Instant.parse("2026-03-13T18:00:00Z"));
        QueryDocuments useCase = new QueryDocuments(
            userRepository,
            documentRepository,
            (query, documentIds) -> {
                clock.advanceMillis(15);
                return List.of(chunk("owned-ready.pdf"));
            },
            (question, context) -> {
                clock.advanceMillis(10);
                return new AnswerGenerator.GeneratedAnswer("Late answer", referencesFrom(context));
            },
            clock
        );

        QueryDocumentsOutput output = useCase.execute(new QueryDocumentsInput(userId, "Slow response", 20));

        assertFalse(output.success());
        assertEquals("Response time exceeded limit", output.errorMessage());
        assertEquals(25, output.responseTimeMs());
    }

    @Test
    void shouldRejectBlankQuestion() {
        QueryDocuments useCase = new QueryDocuments(
            new InMemoryUserRepository(),
            new InMemoryDocumentRepository(),
            (query, documentIds) -> List.of(),
            (question, context) -> new AnswerGenerator.GeneratedAnswer("unused", List.of()),
            Clock.systemUTC()
        );

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> useCase.execute(new QueryDocumentsInput(UUID.randomUUID(), "   ")));

        assertEquals("question must not be null or empty", exception.getMessage());
    }

    private static User user(UUID userId, UserRole role) {
        return new User(userId, "user-" + userId, "user" + userId + "@example.com", role,
            Instant.parse("2026-03-13T10:00:00Z"), true);
    }

    private static Document document(UUID documentId, UUID userId, DocumentStatus status, String fileName) {
        return new Document(documentId, new DocumentMetadata(fileName, 128L, FileType.PDF, "hash-" + documentId),
            userId.toString(), Instant.parse("2026-03-13T12:00:00Z"), status);
    }

    private static DocumentChunk chunk(String documentName) {
        return new DocumentChunk(UUID.randomUUID(), documentName, "paragraph-1", "context", 0.95d);
    }

    private static List<DocumentReference> referencesFrom(List<DocumentChunk> context) {
        return context.stream()
            .map(chunk -> new DocumentReference(chunk.documentId(), chunk.documentName(), chunk.paragraphReference(), chunk.relevanceScore()))
            .toList();
    }

    private static final class RecordingSemanticSearch implements SemanticSearch {
        private final List<DocumentChunk> result;
        private final List<String> documentIds = new ArrayList<>();

        private RecordingSemanticSearch(List<DocumentChunk> result) {
            this.result = result;
        }

        @Override
        public List<DocumentChunk> searchDocuments(String query, List<String> documentIds) {
            this.documentIds.addAll(documentIds);
            return result;
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

    private static final class MutableClock extends Clock {
        private Instant current;

        private MutableClock(Instant current) {
            this.current = current;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return current;
        }

        void advanceMillis(long millis) {
            current = current.plusMillis(millis);
        }
    }
}
