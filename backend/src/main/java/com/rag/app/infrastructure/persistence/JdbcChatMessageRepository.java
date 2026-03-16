package com.rag.app.infrastructure.persistence;

import com.rag.app.domain.entities.ChatMessage;
import com.rag.app.domain.valueobjects.DocumentReference;
import com.rag.app.usecases.repositories.ChatMessageRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public final class JdbcChatMessageRepository implements ChatMessageRepository {
    private static final String INSERT_MESSAGE_SQL = """
        INSERT INTO chat_messages (message_id, user_id, question, answer, created_at, response_time_ms)
        VALUES (?, ?, ?, ?, ?, ?)
        """;
    private static final String UPDATE_MESSAGE_SQL = """
        UPDATE chat_messages
        SET user_id = ?, question = ?, answer = ?, created_at = ?, response_time_ms = ?
        WHERE message_id = ?
        """;
    private static final String DELETE_REFERENCES_SQL = "DELETE FROM document_references WHERE message_id = ?";
    private static final String INSERT_REFERENCE_SQL = """
        INSERT INTO document_references (reference_id, message_id, reference_index, document_id, document_name, paragraph_reference, relevance_score)
        VALUES (?, ?, ?, ?, ?, ?, ?)
        """;
    private static final String FIND_BY_ID_SQL = """
        SELECT message_id, user_id, question, answer, created_at, response_time_ms
        FROM chat_messages
        WHERE message_id = ?
        """;
    private static final String FIND_BY_USER_ID_SQL = """
        SELECT message_id, user_id, question, answer, created_at, response_time_ms
        FROM chat_messages
        WHERE user_id = ?
        ORDER BY created_at ASC, message_id ASC
        """;
    private static final String FIND_RECENT_BY_USER_ID_SQL = """
        SELECT message_id, user_id, question, answer, created_at, response_time_ms
        FROM chat_messages
        WHERE user_id = ?
        ORDER BY created_at DESC, message_id DESC
        LIMIT ?
        """;
    private static final String FIND_REFERENCES_BY_MESSAGE_ID_SQL = """
        SELECT document_id, document_name, paragraph_reference, relevance_score
        FROM document_references
        WHERE message_id = ?
        ORDER BY reference_index ASC
        """;

    private final DataSource dataSource;
    private final ChatMessageRowMapper chatMessageRowMapper;
    private final DocumentReferenceRowMapper documentReferenceRowMapper;

    @Inject
    public JdbcChatMessageRepository(DataSource dataSource) {
        this(dataSource, new ChatMessageRowMapper(), new DocumentReferenceRowMapper());
    }

    public JdbcChatMessageRepository(DataSource dataSource,
                                     ChatMessageRowMapper chatMessageRowMapper,
                                     DocumentReferenceRowMapper documentReferenceRowMapper) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
        this.chatMessageRowMapper = Objects.requireNonNull(chatMessageRowMapper, "chatMessageRowMapper must not be null");
        this.documentReferenceRowMapper = Objects.requireNonNull(documentReferenceRowMapper, "documentReferenceRowMapper must not be null");
    }

    @Override
    public ChatMessage save(ChatMessage message) {
        Objects.requireNonNull(message, "message must not be null");

        try (Connection connection = dataSource.getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            try {
                saveMessage(connection, message);
                replaceReferences(connection, message);
                connection.commit();
                return message;
            } catch (SQLException exception) {
                rollbackQuietly(connection);
                throw exception;
            } finally {
                connection.setAutoCommit(originalAutoCommit);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to save chat message", exception);
        }
    }

    @Override
    public Optional<ChatMessage> findById(UUID messageId) {
        Objects.requireNonNull(messageId, "messageId must not be null");

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_BY_ID_SQL)) {
            statement.setString(1, messageId.toString());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }

                return Optional.of(chatMessageRowMapper.map(resultSet, findReferences(messageId)));
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to find chat message by id", exception);
        }
    }

    @Override
    public List<ChatMessage> findByUserId(UUID userId) {
        Objects.requireNonNull(userId, "userId must not be null");
        return findMessages(userId, FIND_BY_USER_ID_SQL, null);
    }

    @Override
    public List<ChatMessage> findRecentByUserId(UUID userId, int limit) {
        Objects.requireNonNull(userId, "userId must not be null");
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be positive");
        }
        return findMessages(userId, FIND_RECENT_BY_USER_ID_SQL, limit);
    }

    private List<ChatMessage> findMessages(UUID userId, String sql, Integer limit) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, userId.toString());
            if (limit != null) {
                statement.setInt(2, limit);
            }

            try (ResultSet resultSet = statement.executeQuery()) {
                List<ChatMessage> messages = new ArrayList<>();
                while (resultSet.next()) {
                    UUID messageId = UUID.fromString(resultSet.getString("message_id"));
                    messages.add(chatMessageRowMapper.map(resultSet, findReferences(messageId)));
                }
                return messages;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to find chat messages by user id", exception);
        }
    }

    private void saveMessage(Connection connection, ChatMessage message) throws SQLException {
        try (PreparedStatement updateStatement = connection.prepareStatement(UPDATE_MESSAGE_SQL)) {
            updateStatement.setString(1, message.userId().toString());
            updateStatement.setString(2, message.question());
            updateStatement.setString(3, message.answer());
            updateStatement.setTimestamp(4, Timestamp.from(message.createdAt()));
            updateStatement.setLong(5, message.responseTimeMs());
            updateStatement.setString(6, message.messageId().toString());

            if (updateStatement.executeUpdate() > 0) {
                return;
            }
        }

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

    private void replaceReferences(Connection connection, ChatMessage message) throws SQLException {
        try (PreparedStatement deleteStatement = connection.prepareStatement(DELETE_REFERENCES_SQL)) {
            deleteStatement.setString(1, message.messageId().toString());
            deleteStatement.executeUpdate();
        }

        try (PreparedStatement insertStatement = connection.prepareStatement(INSERT_REFERENCE_SQL)) {
            int index = 0;
            for (DocumentReference reference : message.documentReferences()) {
                insertStatement.setString(1, message.messageId() + "-" + index);
                insertStatement.setString(2, message.messageId().toString());
                insertStatement.setInt(3, index);
                insertStatement.setString(4, reference.documentId().toString());
                insertStatement.setString(5, reference.documentName());
                insertStatement.setString(6, reference.paragraphReference());
                insertStatement.setBigDecimal(7, java.math.BigDecimal.valueOf(reference.relevanceScore()));
                insertStatement.addBatch();
                index++;
            }
            insertStatement.executeBatch();
        }
    }

    private List<DocumentReference> findReferences(UUID messageId) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_REFERENCES_BY_MESSAGE_ID_SQL)) {
            statement.setString(1, messageId.toString());

            try (ResultSet resultSet = statement.executeQuery()) {
                List<DocumentReference> references = new ArrayList<>();
                while (resultSet.next()) {
                    references.add(documentReferenceRowMapper.map(resultSet));
                }
                return references;
            }
        }
    }

    private void rollbackQuietly(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException ignored) {
        }
    }
}
