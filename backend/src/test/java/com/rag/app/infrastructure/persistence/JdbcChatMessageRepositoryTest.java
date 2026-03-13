package com.rag.app.infrastructure.persistence;

import com.rag.app.domain.entities.ChatMessage;
import com.rag.app.domain.valueobjects.DocumentReference;
import com.rag.app.usecases.repositories.ChatMessageRepository;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcChatMessageRepositoryTest {
    private JdbcDataSource dataSource;
    private ChatMessageRepository repository;

    @BeforeEach
    void setUp() throws SQLException, IOException {
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:chat-message-repository;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE");
        dataSource.setUser("sa");
        dataSource.setPassword("sa");
        executeSchema();
        repository = new JdbcChatMessageRepository(dataSource);
    }

    @Test
    void shouldSaveAndFindMessageWithDocumentReferences() {
        ChatMessage message = message(
            UUID.randomUUID(),
            UUID.randomUUID(),
            Instant.parse("2026-03-13T18:00:00Z"),
            List.of(
                new DocumentReference(UUID.randomUUID(), "guide.pdf", "p-10", 0.9123d),
                new DocumentReference(UUID.randomUUID(), "faq.pdf", "p-2", 0.7345d)
            )
        );

        repository.save(message);

        ChatMessage stored = repository.findById(message.messageId()).orElseThrow();

        assertEquals(message.messageId(), stored.messageId());
        assertEquals(message.userId(), stored.userId());
        assertEquals(message.question(), stored.question());
        assertEquals(message.answer(), stored.answer());
        assertEquals(message.createdAt(), stored.createdAt());
        assertEquals(message.responseTimeMs(), stored.responseTimeMs());
        assertEquals(message.documentReferences(), stored.documentReferences());
    }

    @Test
    void shouldReturnAllMessagesForUser() {
        UUID userId = UUID.randomUUID();
        repository.save(message(UUID.randomUUID(), userId, Instant.parse("2026-03-13T10:00:00Z"), List.of()));
        repository.save(message(UUID.randomUUID(), userId, Instant.parse("2026-03-13T11:00:00Z"), List.of()));
        repository.save(message(UUID.randomUUID(), UUID.randomUUID(), Instant.parse("2026-03-13T12:00:00Z"), List.of()));

        List<ChatMessage> messages = repository.findByUserId(userId);

        assertEquals(2, messages.size());
        assertTrue(messages.get(0).createdAt().isBefore(messages.get(1).createdAt()));
        assertEquals(userId, messages.get(0).userId());
        assertEquals(userId, messages.get(1).userId());
    }

    @Test
    void shouldReturnMostRecentMessagesUpToLimit() {
        UUID userId = UUID.randomUUID();
        ChatMessage oldest = message(UUID.randomUUID(), userId, Instant.parse("2026-03-13T09:00:00Z"), List.of());
        ChatMessage middle = message(UUID.randomUUID(), userId, Instant.parse("2026-03-13T10:00:00Z"), List.of());
        ChatMessage newest = message(UUID.randomUUID(), userId, Instant.parse("2026-03-13T11:00:00Z"), List.of());
        repository.save(oldest);
        repository.save(middle);
        repository.save(newest);

        List<ChatMessage> messages = repository.findRecentByUserId(userId, 2);

        assertEquals(List.of(newest.messageId(), middle.messageId()), messages.stream().map(ChatMessage::messageId).toList());
    }

    @Test
    void shouldRollbackMessageInsertWhenReferenceInsertFails() {
        UUID messageId = UUID.randomUUID();
        ChatMessage invalidMessage = message(
            messageId,
            UUID.randomUUID(),
            Instant.parse("2026-03-13T18:30:00Z"),
            List.of(new DocumentReference(UUID.randomUUID(), "x".repeat(501), "p-1", 0.55d))
        );

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> repository.save(invalidMessage));

        assertTrue(exception.getMessage().contains("Failed to save chat message"));
        assertFalse(repository.findById(messageId).isPresent());
    }

    @Test
    void shouldRejectNonPositiveRecentLimit() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> repository.findRecentByUserId(UUID.randomUUID(), 0));

        assertEquals("limit must be positive", exception.getMessage());
    }

    private void executeSchema() throws SQLException, IOException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("DROP ALL OBJECTS");
            String schemaSql = new String(
                JdbcChatMessageRepositoryTest.class.getResourceAsStream("/schema.sql").readAllBytes(),
                StandardCharsets.UTF_8
            );

            for (String sqlStatement : schemaSql.split(";")) {
                String trimmedStatement = sqlStatement.trim();
                if (!trimmedStatement.isEmpty()) {
                    statement.execute(trimmedStatement);
                }
            }
        }
    }

    private static ChatMessage message(UUID messageId, UUID userId, Instant createdAt, List<DocumentReference> references) {
        return new ChatMessage(
            messageId,
            userId,
            "What changed?",
            "Here is the latest answer.",
            references,
            createdAt,
            125L
        );
    }
}
