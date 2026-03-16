package com.rag.app.infrastructure.persistence;

import com.rag.app.domain.valueobjects.AnswerSourceReference;
import com.rag.app.usecases.repositories.AnswerSourceReferenceRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@ApplicationScoped
public class JdbcAnswerSourceReferenceRepository implements AnswerSourceReferenceRepository {
    private static final String DELETE_BY_ANSWER_ID_SQL = "DELETE FROM answer_source_references WHERE answer_id = ?";
    private static final String INSERT_SQL = """
        INSERT INTO answer_source_references (
            reference_id, answer_id, document_id, chunk_id, snippet_content, snippet_context,
            start_position, end_position, relevance_score, source_order,
            document_title, document_filename, document_file_type, page_number,
            chunk_index, created_at
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
    private static final String FIND_BY_ANSWER_ID_SQL = """
        SELECT reference_id, answer_id, document_id, chunk_id, snippet_content, snippet_context,
               start_position, end_position, relevance_score, source_order,
               document_title, document_filename, document_file_type, page_number,
               chunk_index, created_at
        FROM answer_source_references
        WHERE answer_id = ?
        ORDER BY source_order ASC, created_at ASC
        """;

    private final DataSource dataSource;

    @Inject
    public JdbcAnswerSourceReferenceRepository(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
    }

    @Override
    public void replaceForAnswer(UUID answerId, List<AnswerSourceReference> references) {
        Objects.requireNonNull(answerId, "answerId must not be null");

        try (Connection connection = dataSource.getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            try {
                replaceForAnswer(connection, answerId, references);
                connection.commit();
            } catch (SQLException exception) {
                rollbackQuietly(connection);
                throw exception;
            } finally {
                connection.setAutoCommit(originalAutoCommit);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to save answer source references", exception);
        }
    }

    void replaceForAnswer(Connection connection, UUID answerId, List<AnswerSourceReference> references) throws SQLException {
        try (PreparedStatement deleteStatement = connection.prepareStatement(DELETE_BY_ANSWER_ID_SQL)) {
            deleteStatement.setString(1, answerId.toString());
            deleteStatement.executeUpdate();
        }

        if (references == null || references.isEmpty()) {
            return;
        }

        try (PreparedStatement insertStatement = connection.prepareStatement(INSERT_SQL)) {
            for (AnswerSourceReference reference : references) {
                insertStatement.setString(1, reference.referenceId().toString());
                insertStatement.setString(2, reference.answerId().toString());
                insertStatement.setString(3, reference.documentId().toString());
                insertStatement.setString(4, reference.chunkId());
                insertStatement.setString(5, reference.snippetContent());
                insertStatement.setString(6, reference.snippetContext());
                setNullableInteger(insertStatement, 7, reference.startPosition());
                setNullableInteger(insertStatement, 8, reference.endPosition());
                insertStatement.setBigDecimal(9, java.math.BigDecimal.valueOf(reference.relevanceScore()));
                insertStatement.setInt(10, reference.sourceOrder());
                insertStatement.setString(11, reference.documentTitle());
                insertStatement.setString(12, reference.documentFilename());
                insertStatement.setString(13, reference.documentFileType());
                setNullableInteger(insertStatement, 14, reference.pageNumber());
                setNullableInteger(insertStatement, 15, reference.chunkIndex());
                insertStatement.setTimestamp(16, Timestamp.from(reference.createdAt()));
                insertStatement.addBatch();
            }
            insertStatement.executeBatch();
        }
    }

    @Override
    public List<AnswerSourceReference> findByAnswerId(UUID answerId) {
        Objects.requireNonNull(answerId, "answerId must not be null");

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_BY_ANSWER_ID_SQL)) {
            statement.setString(1, answerId.toString());

            try (ResultSet resultSet = statement.executeQuery()) {
                List<AnswerSourceReference> references = new ArrayList<>();
                while (resultSet.next()) {
                    references.add(map(resultSet));
                }
                return references;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load answer source references", exception);
        }
    }

    private AnswerSourceReference map(ResultSet resultSet) throws SQLException {
        return new AnswerSourceReference(
            UUID.fromString(resultSet.getString("reference_id")),
            UUID.fromString(resultSet.getString("answer_id")),
            UUID.fromString(resultSet.getString("document_id")),
            resultSet.getString("chunk_id"),
            resultSet.getString("snippet_content"),
            resultSet.getString("snippet_context"),
            nullableInteger(resultSet, "start_position"),
            nullableInteger(resultSet, "end_position"),
            resultSet.getBigDecimal("relevance_score").doubleValue(),
            resultSet.getInt("source_order"),
            resultSet.getString("document_title"),
            resultSet.getString("document_filename"),
            resultSet.getString("document_file_type"),
            nullableInteger(resultSet, "page_number"),
            nullableInteger(resultSet, "chunk_index"),
            resultSet.getTimestamp("created_at").toInstant()
        );
    }

    private void setNullableInteger(PreparedStatement statement, int index, Integer value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.INTEGER);
        } else {
            statement.setInt(index, value);
        }
    }

    private Integer nullableInteger(ResultSet resultSet, String columnName) throws SQLException {
        int value = resultSet.getInt(columnName);
        return resultSet.wasNull() ? null : value;
    }

    private void rollbackQuietly(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException ignored) {
        }
    }
}
