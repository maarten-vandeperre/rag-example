package com.rag.app.infrastructure.persistence;

import com.rag.app.domain.entities.ChatMessage;
import com.rag.app.domain.valueobjects.AnswerSourceReference;
import com.rag.app.domain.valueobjects.DocumentReference;
import com.rag.app.usecases.interfaces.AnswerPersistence;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;

@ApplicationScoped
public class JdbcAnswerPersistence implements AnswerPersistence {
    private static final String INSERT_MESSAGE_SQL = """
        INSERT INTO chat_messages (message_id, user_id, question, answer, created_at, response_time_ms)
        VALUES (?, ?, ?, ?, ?, ?)
        """;
    private static final String INSERT_REFERENCE_SQL = """
        INSERT INTO document_references (reference_id, message_id, reference_index, document_id, document_name, paragraph_reference, relevance_score)
        VALUES (?, ?, ?, ?, ?, ?, ?)
        """;

    private final DataSource dataSource;
    private final JdbcAnswerSourceReferenceRepository answerSourceReferenceRepository;

    @Inject
    public JdbcAnswerPersistence(DataSource dataSource,
                                 JdbcAnswerSourceReferenceRepository answerSourceReferenceRepository) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
        this.answerSourceReferenceRepository = Objects.requireNonNull(answerSourceReferenceRepository, "answerSourceReferenceRepository must not be null");
    }

    @Override
    public void persist(ChatMessage message, List<AnswerSourceReference> sourceReferences) {
        Objects.requireNonNull(message, "message must not be null");

        try (Connection connection = dataSource.getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            try {
                insertMessage(connection, message);
                insertReferences(connection, message);
                answerSourceReferenceRepository.replaceForAnswer(connection, message.messageId(), sourceReferences);
                connection.commit();
            } catch (SQLException exception) {
                rollbackQuietly(connection);
                throw exception;
            } finally {
                connection.setAutoCommit(originalAutoCommit);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to persist answer and source references", exception);
        }
    }

    private void insertMessage(Connection connection, ChatMessage message) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(INSERT_MESSAGE_SQL)) {
            statement.setString(1, message.messageId().toString());
            statement.setString(2, message.userId().toString());
            statement.setString(3, message.question());
            statement.setString(4, message.answer());
            statement.setTimestamp(5, Timestamp.from(message.createdAt()));
            statement.setLong(6, message.responseTimeMs());
            statement.executeUpdate();
        }
    }

    private void insertReferences(Connection connection, ChatMessage message) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(INSERT_REFERENCE_SQL)) {
            int index = 0;
            for (DocumentReference reference : message.documentReferences()) {
                statement.setString(1, message.messageId() + "-" + index);
                statement.setString(2, message.messageId().toString());
                statement.setInt(3, index);
                statement.setString(4, reference.documentId().toString());
                statement.setString(5, reference.documentName());
                statement.setString(6, reference.paragraphReference());
                statement.setBigDecimal(7, java.math.BigDecimal.valueOf(reference.relevanceScore()));
                statement.addBatch();
                index++;
            }
            statement.executeBatch();
        }
    }

    private void rollbackQuietly(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException ignored) {
        }
    }
}
