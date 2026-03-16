package com.rag.app.usecases;

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
import com.rag.app.usecases.interfaces.DocumentChunkStore;
import com.rag.app.usecases.models.AnswerSourceDetailsOutput;
import com.rag.app.usecases.models.DocumentChunk;
import com.rag.app.usecases.repositories.AnswerSourceReferenceRepository;
import com.rag.app.usecases.repositories.ChatMessageRepository;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GetAnswerSourceDetailsTest {

    @Test
    void shouldReturnOrderedSourceDetailsForAccessibleAnswer() {
        UUID userId = UUID.randomUUID();
        UUID answerId = UUID.randomUUID();
        UUID primaryDocumentId = UUID.randomUUID();
        UUID secondaryDocumentId = UUID.randomUUID();

        InMemoryUserRepository userRepository = new InMemoryUserRepository();
        userRepository.store(user(userId, UserRole.STANDARD));

        InMemoryDocumentRepository documentRepository = new InMemoryDocumentRepository();
        documentRepository.store(document(primaryDocumentId, userId, "guide.pdf", DocumentStatus.READY));
        documentRepository.store(document(secondaryDocumentId, userId, "faq.md", DocumentStatus.READY));

        InMemoryChatMessageRepository chatMessageRepository = new InMemoryChatMessageRepository();
        chatMessageRepository.store(new ChatMessage(
            answerId,
            userId,
            "How do uploads work?",
            "Uploads are indexed after processing.",
            List.of(
                new DocumentReference(primaryDocumentId, "guide.pdf", "chunk-1", 0.95d),
                new DocumentReference(secondaryDocumentId, "faq.md", "chunk-2", 0.81d)
            ),
            Instant.parse("2026-03-16T09:30:00Z"),
            42L
        ));

        GetAnswerSourceDetails useCase = new GetAnswerSourceDetails(
            chatMessageRepository,
            documentRepository,
            userRepository,
            new InMemoryAnswerSourceReferenceRepository(Map.of()),
            new InMemoryAnswerSourceChunkStore(Map.of()),
            new InMemoryChunkStore(Map.of(
                primaryDocumentId, List.of(
                    chunk(primaryDocumentId, "guide.pdf", 0, "chunk-1", "Uploads are parsed before indexing."),
                    chunk(primaryDocumentId, "guide.pdf", 1, "chunk-2", "Indexed files become searchable.")
                ),
                secondaryDocumentId, List.of(
                    chunk(secondaryDocumentId, "faq.md", 0, "chunk-1", "Markdown files use the same flow."),
                    chunk(secondaryDocumentId, "faq.md", 1, "chunk-2", "Answers can cite markdown notes too.")
                )
            ))
        );

        AnswerSourceDetailsOutput output = useCase.execute(answerId, userId);

        assertEquals(answerId, output.answerId());
        assertEquals(2, output.totalSources());
        assertEquals(2, output.availableSources());
        assertEquals("guide.pdf", output.sources().get(0).fileName());
        assertEquals("Uploads are parsed before indexing.", output.sources().get(0).snippet().content());
        assertTrue(output.sources().get(0).available());
        assertEquals("faq.md", output.sources().get(1).fileName());
        assertEquals(Integer.valueOf(1), output.sources().get(1).metadata().chunkIndex());
    }

    @Test
    void shouldMarkMissingDocumentsAsUnavailable() {
        UUID userId = UUID.randomUUID();
        UUID answerId = UUID.randomUUID();
        UUID missingDocumentId = UUID.randomUUID();

        InMemoryUserRepository userRepository = new InMemoryUserRepository();
        userRepository.store(user(userId, UserRole.STANDARD));

        InMemoryChatMessageRepository chatMessageRepository = new InMemoryChatMessageRepository();
        chatMessageRepository.store(new ChatMessage(
            answerId,
            userId,
            "What sources were used?",
            "One source is missing.",
            List.of(new DocumentReference(missingDocumentId, "missing.pdf", "chunk-1", 0.73d)),
            Instant.parse("2026-03-16T09:30:00Z"),
            30L
        ));

        GetAnswerSourceDetails useCase = new GetAnswerSourceDetails(
            chatMessageRepository,
            new InMemoryDocumentRepository(),
            userRepository,
            new InMemoryAnswerSourceReferenceRepository(Map.of()),
            new InMemoryAnswerSourceChunkStore(Map.of()),
            new InMemoryChunkStore(Map.of())
        );

        AnswerSourceDetailsOutput output = useCase.execute(answerId, userId);

        assertEquals(1, output.totalSources());
        assertEquals(0, output.availableSources());
        assertFalse(output.sources().get(0).available());
        assertEquals("missing.pdf", output.sources().get(0).fileName());
    }

    @Test
    void shouldRejectAccessToOtherUsersAnswerForStandardUser() {
        UUID ownerId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        UUID answerId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();

        InMemoryUserRepository userRepository = new InMemoryUserRepository();
        userRepository.store(user(ownerId, UserRole.STANDARD));
        userRepository.store(user(otherUserId, UserRole.STANDARD));

        InMemoryDocumentRepository documentRepository = new InMemoryDocumentRepository();
        documentRepository.store(document(documentId, ownerId, "guide.pdf", DocumentStatus.READY));

        InMemoryChatMessageRepository chatMessageRepository = new InMemoryChatMessageRepository();
        chatMessageRepository.store(new ChatMessage(
            answerId,
            ownerId,
            "Question",
            "Answer",
            List.of(new DocumentReference(documentId, "guide.pdf", "chunk-1", 0.95d)),
            Instant.parse("2026-03-16T09:30:00Z"),
            15L
        ));

        GetAnswerSourceDetails useCase = new GetAnswerSourceDetails(
            chatMessageRepository,
            documentRepository,
            userRepository,
            new InMemoryAnswerSourceReferenceRepository(Map.of()),
            new InMemoryAnswerSourceChunkStore(Map.of()),
            new InMemoryChunkStore(Map.of(documentId, List.of(chunk(documentId, "guide.pdf", 0, "chunk-1", "content"))))
        );

        assertThrows(SecurityException.class, () -> useCase.execute(answerId, otherUserId));
    }

    @Test
    void shouldUseAnswerScopedChunkWhenStoredReferenceLabelDoesNotExactlyMatchChunkReference() {
        UUID userId = UUID.randomUUID();
        UUID answerId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();

        InMemoryUserRepository userRepository = new InMemoryUserRepository();
        userRepository.store(user(userId, UserRole.STANDARD));

        InMemoryDocumentRepository documentRepository = new InMemoryDocumentRepository();
        documentRepository.store(document(documentId, userId, "guide.pdf", DocumentStatus.READY));

        InMemoryChatMessageRepository chatMessageRepository = new InMemoryChatMessageRepository();
        chatMessageRepository.store(new ChatMessage(
            answerId,
            userId,
            "Show me the indexed step",
            "The answer refers to the second chunk.",
            List.of(new DocumentReference(documentId, "guide.pdf", "Paragraph 2", 0.91d)),
            Instant.parse("2026-03-16T09:30:00Z"),
            20L
        ));

        GetAnswerSourceDetails useCase = new GetAnswerSourceDetails(
            chatMessageRepository,
            documentRepository,
            userRepository,
            new InMemoryAnswerSourceReferenceRepository(Map.of()),
            new InMemoryAnswerSourceChunkStore(Map.of(answerId, List.of(
                chunk(documentId, "guide.pdf", 1, "chunk-2", "This is the actual retrieved chunk for the answer.")
            ))),
            new InMemoryChunkStore(Map.of())
        );

        AnswerSourceDetailsOutput output = useCase.execute(answerId, userId);

        assertEquals(1, output.availableSources());
        assertEquals("This is the actual retrieved chunk for the answer.", output.sources().get(0).snippet().content());
        assertEquals(Integer.valueOf(1), output.sources().get(0).metadata().chunkIndex());
    }

    @Test
    void shouldFallbackToFirstAvailableChunkWhenReferenceCannotBeMatched() {
        UUID userId = UUID.randomUUID();
        UUID answerId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();

        InMemoryUserRepository userRepository = new InMemoryUserRepository();
        userRepository.store(user(userId, UserRole.STANDARD));

        InMemoryDocumentRepository documentRepository = new InMemoryDocumentRepository();
        documentRepository.store(document(documentId, userId, "guide.pdf", DocumentStatus.READY));

        InMemoryChatMessageRepository chatMessageRepository = new InMemoryChatMessageRepository();
        chatMessageRepository.store(new ChatMessage(
            answerId,
            userId,
            "Which chunk was used?",
            "A chunk should still be returned.",
            List.of(new DocumentReference(documentId, "guide.pdf", "Unmapped reference", 0.6d)),
            Instant.parse("2026-03-16T09:30:00Z"),
            20L
        ));

        GetAnswerSourceDetails useCase = new GetAnswerSourceDetails(
            chatMessageRepository,
            documentRepository,
            userRepository,
            new InMemoryAnswerSourceReferenceRepository(Map.of()),
            new InMemoryAnswerSourceChunkStore(Map.of()),
            new InMemoryChunkStore(Map.of(documentId, List.of(
                chunk(documentId, "guide.pdf", 0, "chunk-1", "Fallback first chunk content."),
                chunk(documentId, "guide.pdf", 1, "chunk-2", "Fallback second chunk content.")
            )))
        );

        AnswerSourceDetailsOutput output = useCase.execute(answerId, userId);

        assertTrue(output.sources().get(0).available());
        assertEquals("Fallback first chunk content.", output.sources().get(0).snippet().content());
    }

    @Test
    void shouldReturnPersistedSnippetWhenSourceReferencesExist() {
        UUID userId = UUID.randomUUID();
        UUID answerId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();

        InMemoryUserRepository userRepository = new InMemoryUserRepository();
        userRepository.store(user(userId, UserRole.STANDARD));

        InMemoryDocumentRepository documentRepository = new InMemoryDocumentRepository();
        documentRepository.store(document(documentId, userId, "guide.pdf", DocumentStatus.READY));

        InMemoryChatMessageRepository chatMessageRepository = new InMemoryChatMessageRepository();
        chatMessageRepository.store(new ChatMessage(
            answerId,
            userId,
            "Show me the persisted snippet",
            "Persisted answers should expose stored source text.",
            List.of(new DocumentReference(documentId, "guide.pdf", "chunk-1", 0.97d)),
            Instant.parse("2026-03-16T09:30:00Z"),
            20L
        ));

        Map<UUID, List<AnswerSourceReference>> persistedReferences = Map.of(
            answerId,
            List.of(new AnswerSourceReference(
                UUID.randomUUID(),
                answerId,
                documentId,
                "guide-chunk-1",
                "Persisted chunk content for the answer.",
                "Persisted chunk context for the answer.",
                0,
                39,
                0.97d,
                0,
                "guide.pdf",
                "guide.pdf",
                "PDF",
                null,
                0,
                Instant.parse("2026-03-16T09:30:00Z")
            ))
        );

        GetAnswerSourceDetails useCase = new GetAnswerSourceDetails(
            chatMessageRepository,
            documentRepository,
            userRepository,
            new InMemoryAnswerSourceReferenceRepository(persistedReferences),
            new InMemoryAnswerSourceChunkStore(Map.of()),
            new InMemoryChunkStore(Map.of())
        );

        AnswerSourceDetailsOutput output = useCase.execute(answerId, userId);

        assertEquals(1, output.availableSources());
        assertEquals("Persisted chunk content for the answer.", output.sources().get(0).snippet().content());
        assertEquals("guide-chunk-1", output.sources().get(0).sourceId());
    }

    private static User user(UUID userId, UserRole role) {
        return new User(userId, "user-" + userId, userId + "@example.com", role, Instant.parse("2026-03-16T08:00:00Z"), true);
    }

    private static Document document(UUID documentId, UUID userId, String fileName, DocumentStatus status) {
        return new Document(documentId, new DocumentMetadata(fileName, 256L, FileType.PDF, "hash-" + documentId),
            userId.toString(), Instant.parse("2026-03-16T08:30:00Z"), status);
    }

    private static DocumentChunk chunk(UUID documentId, String documentName, int chunkIndex, String paragraphReference, String text) {
        return new DocumentChunk(documentId + "-chunk-" + chunkIndex, documentId, documentName, chunkIndex, paragraphReference, text, new double[0], 0.9d);
    }

    private static final class InMemoryChunkStore implements DocumentChunkStore {
        private final Map<UUID, List<DocumentChunk>> chunks;

        private InMemoryChunkStore(Map<UUID, List<DocumentChunk>> chunks) {
            this.chunks = chunks;
        }

        @Override
        public List<DocumentChunk> getDocumentChunks(UUID documentId) {
            return chunks.getOrDefault(documentId, List.of());
        }
    }

    private static final class InMemoryAnswerSourceChunkStore implements AnswerSourceChunkStore {
        private final Map<UUID, List<DocumentChunk>> chunksByAnswerId;

        private InMemoryAnswerSourceChunkStore(Map<UUID, List<DocumentChunk>> chunksByAnswerId) {
            this.chunksByAnswerId = chunksByAnswerId;
        }

        @Override
        public void store(UUID answerId, List<DocumentChunk> chunks) {
            throw new UnsupportedOperationException("test helper only");
        }

        @Override
        public List<DocumentChunk> getChunks(UUID answerId) {
            return chunksByAnswerId.getOrDefault(answerId, List.of());
        }
    }

    private static final class InMemoryAnswerSourceReferenceRepository implements AnswerSourceReferenceRepository {
        private final Map<UUID, List<AnswerSourceReference>> referencesByAnswerId;

        private InMemoryAnswerSourceReferenceRepository(Map<UUID, List<AnswerSourceReference>> referencesByAnswerId) {
            this.referencesByAnswerId = referencesByAnswerId;
        }

        @Override
        public void replaceForAnswer(UUID answerId, List<AnswerSourceReference> references) {
            throw new UnsupportedOperationException("test helper only");
        }

        @Override
        public List<AnswerSourceReference> findByAnswerId(UUID answerId) {
            return referencesByAnswerId.getOrDefault(answerId, List.of());
        }
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
