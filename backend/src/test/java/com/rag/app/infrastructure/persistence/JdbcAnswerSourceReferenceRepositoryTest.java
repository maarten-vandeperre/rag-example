package com.rag.app.infrastructure.persistence;

import com.rag.app.domain.valueobjects.AnswerSourceReference;
import com.rag.app.usecases.repositories.AnswerSourceReferenceRepository;
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

class JdbcAnswerSourceReferenceRepositoryTest {
    private JdbcDataSource dataSource;
    private AnswerSourceReferenceRepository repository;

    @BeforeEach
    void setUp() throws SQLException, IOException {
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:answer-source-reference-repository;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE");
        dataSource.setUser("sa");
        dataSource.setPassword("sa");
        executeSchema();
        repository = new JdbcAnswerSourceReferenceRepository(dataSource);
    }

    @Test
    void shouldSaveAndLoadAnswerSourceReferencesInOrder() {
        UUID answerId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        insertChatMessage(answerId);

        repository.replaceForAnswer(answerId, List.of(
            reference(answerId, documentId, "chunk-2", "Second chunk", 0.82d, 1),
            reference(answerId, documentId, "chunk-1", "First chunk", 0.94d, 0)
        ));

        List<AnswerSourceReference> loaded = repository.findByAnswerId(answerId);

        assertEquals(2, loaded.size());
        assertEquals("chunk-1", loaded.get(0).chunkId());
        assertEquals("First chunk", loaded.get(0).snippetContent());
        assertEquals("chunk-2", loaded.get(1).chunkId());
    }

    @Test
    void shouldReplaceExistingReferencesForAnswer() {
        UUID answerId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        insertChatMessage(answerId);

        repository.replaceForAnswer(answerId, List.of(reference(answerId, documentId, "chunk-1", "Old chunk", 0.91d, 0)));
        repository.replaceForAnswer(answerId, List.of(reference(answerId, documentId, "chunk-3", "New chunk", 0.96d, 0)));

        List<AnswerSourceReference> loaded = repository.findByAnswerId(answerId);

        assertEquals(1, loaded.size());
        assertEquals("chunk-3", loaded.get(0).chunkId());
        assertEquals("New chunk", loaded.get(0).snippetContent());
    }

    private void executeSchema() throws SQLException, IOException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("DROP ALL OBJECTS");
            String schemaSql = new String(
                JdbcAnswerSourceReferenceRepositoryTest.class.getResourceAsStream("/schema.sql").readAllBytes(),
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

    private AnswerSourceReference reference(UUID answerId,
                                            UUID documentId,
                                            String chunkId,
                                            String snippetContent,
                                            double relevanceScore,
                                            int sourceOrder) {
        return new AnswerSourceReference(
            UUID.randomUUID(),
            answerId,
            documentId,
            chunkId,
            snippetContent,
            snippetContent + " context",
            0,
            snippetContent.length(),
            relevanceScore,
            sourceOrder,
            "Guide",
            "guide.pdf",
            "PDF",
            null,
            sourceOrder,
            Instant.parse("2026-03-16T10:00:00Z")
        );
    }

    private void insertChatMessage(UUID answerId) {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("""
                INSERT INTO chat_messages (message_id, user_id, question, answer, created_at, response_time_ms)
                VALUES ('%s', '%s', 'Question?', 'Answer.', TIMESTAMP '2026-03-16 10:00:00', 100)
                """.formatted(answerId, UUID.randomUUID()));
        } catch (SQLException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
