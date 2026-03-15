package com.rag.app.chat;

import com.rag.app.chat.domain.entities.ChatMessage;
import com.rag.app.chat.domain.services.ChatDomainService;
import com.rag.app.chat.domain.valueobjects.DocumentReference;
import com.rag.app.chat.domain.valueobjects.UserRole;
import com.rag.app.chat.infrastructure.ChatSystemFacadeImpl;
import com.rag.app.chat.infrastructure.llm.OllamaAnswerGenerator;
import com.rag.app.chat.infrastructure.search.WeaviateVectorStore;
import com.rag.app.chat.interfaces.ChatMessageRepository;
import com.rag.app.chat.interfaces.DocumentAccessService;
import com.rag.app.chat.interfaces.UserContextService;
import com.rag.app.chat.usecases.GenerateAnswer;
import com.rag.app.chat.usecases.GetChatHistory;
import com.rag.app.chat.usecases.QueryDocuments;
import com.rag.app.chat.usecases.models.DocumentChunk;
import com.rag.app.chat.usecases.models.DocumentSummary;
import com.rag.app.chat.usecases.models.GetChatHistoryInput;
import com.rag.app.chat.usecases.models.QueryDocumentsInput;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatSystemFacadeTest {
    @Test
    void shouldQueryDocumentsAndExposeHistoryThroughFacade() {
        InMemoryChatMessageRepository repository = new InMemoryChatMessageRepository();
        WeaviateVectorStore vectorStore = new WeaviateVectorStore();
        UserContextService userContext = new StubUserContextService();
        DocumentAccessService documentAccessService = new StubDocumentAccessService();
        Clock clock = Clock.fixed(Instant.parse("2026-03-14T12:00:00Z"), ZoneOffset.UTC);

        ChatSystemFacadeImpl facade = new ChatSystemFacadeImpl(
            new QueryDocuments(
                userContext,
                documentAccessService,
                vectorStore,
                new GenerateAnswer(new OllamaAnswerGenerator()),
                repository,
                new ChatDomainService(),
                clock
            ),
            new GetChatHistory(repository),
            vectorStore,
            vectorStore
        );

        facade.storeDocumentVectors(DOC_ID.toString(), "uploads are processed into searchable chunks");

        var query = facade.queryDocuments(new QueryDocumentsInput("user-1", "uploads", 100));
        assertTrue(query.success());
        assertEquals(1, query.documentReferences().size());
        assertTrue(query.documentReferences().get(0).documentName().contains(DOC_ID.toString()));

        var search = facade.searchSimilarContent("uploads", List.of(DOC_ID.toString()));
        assertEquals(1, search.size());

        var history = facade.getChatHistory(new GetChatHistoryInput("user-1", 5));
        assertEquals(1, history.messages().size());

        facade.removeDocumentVectors(DOC_ID.toString());
        assertFalse(facade.searchSimilarContent("uploads", List.of(DOC_ID.toString())).stream().findAny().isPresent());
    }

    private static final UUID DOC_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static final class StubUserContextService implements UserContextService {
        @Override
        public UserRole getUserRole(String userId) {
            return UserRole.STANDARD;
        }

        @Override
        public boolean isActiveUser(String userId) {
            return true;
        }
    }

    private static final class StubDocumentAccessService implements DocumentAccessService {
        @Override
        public List<DocumentSummary> getAccessibleDocuments(String userId, UserRole role) {
            return List.of(new DocumentSummary(DOC_ID.toString(), "guide.pdf", true));
        }

        @Override
        public boolean isDocumentAccessible(String documentId, String userId, UserRole role) {
            return DOC_ID.toString().equals(documentId);
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
        public List<ChatMessage> findByUserId(String userId) {
            return messages.values().stream().filter(message -> message.userId().equals(userId)).toList();
        }

        @Override
        public List<ChatMessage> findRecentByUserId(String userId, int limit) {
            return findByUserId(userId).stream().limit(limit).toList();
        }
    }
}
