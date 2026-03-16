package com.rag.app.chat.infrastructure.persistence;

import com.rag.app.chat.domain.entities.AnswerSourceReference;
import com.rag.app.chat.domain.exceptions.RepositoryException;
import com.rag.app.chat.interfaces.AnswerSourceReferenceRepository;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * JDBC implementation of AnswerSourceReferenceRepository.
 * Provides persistent storage for answer-to-chunk relationships.
 */
public final class JdbcAnswerSourceReferenceRepository implements AnswerSourceReferenceRepository {
    
    private final DataSource dataSource;

    public JdbcAnswerSourceReferenceRepository(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
    }

    @Override
    public AnswerSourceReference save(AnswerSourceReference reference) {
        Objects.requireNonNull(reference, "reference must not be null");
        
        String sql = """
            INSERT INTO answer_source_references (
                id, answer_id, document_id, chunk_id, snippet_content, snippet_context,
                start_position, end_position, relevance_score, source_order,
                document_title, document_filename, document_file_type,
                page_number, chunk_index, created_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, reference.getId());
            stmt.setString(2, reference.getAnswerId());
            stmt.setString(3, reference.getDocumentId());
            stmt.setString(4, reference.getChunkId());
            stmt.setString(5, reference.getSnippetContent());
            stmt.setString(6, reference.getSnippetContext());
            setIntegerOrNull(stmt, 7, reference.getStartPosition());
            setIntegerOrNull(stmt, 8, reference.getEndPosition());
            stmt.setDouble(9, reference.getRelevanceScore());
            stmt.setInt(10, reference.getSourceOrder());
            stmt.setString(11, reference.getDocumentTitle());
            stmt.setString(12, reference.getDocumentFilename());
            stmt.setString(13, reference.getDocumentFileType());
            setIntegerOrNull(stmt, 14, reference.getPageNumber());
            setIntegerOrNull(stmt, 15, reference.getChunkIndex());
            stmt.setTimestamp(16, Timestamp.from(reference.getCreatedAt()));
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected != 1) {
                throw new RepositoryException("Failed to save answer source reference: " + reference.getId());
            }
            
            return reference;
            
        } catch (SQLException e) {
            throw new RepositoryException("Database error saving answer source reference: " + reference.getId(), e);
        }
    }

    @Override
    public List<AnswerSourceReference> findByAnswerIdOrderBySourceOrder(String answerId) {
        if (answerId == null || answerId.isBlank()) {
            throw new IllegalArgumentException("answerId must not be null or blank");
        }
        
        String sql = """
            SELECT id, answer_id, document_id, chunk_id, snippet_content, snippet_context,
                   start_position, end_position, relevance_score, source_order,
                   document_title, document_filename, document_file_type,
                   page_number, chunk_index, created_at
            FROM answer_source_references
            WHERE answer_id = ?
            ORDER BY source_order ASC
            """;
        
        List<AnswerSourceReference> references = new ArrayList<>();
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, answerId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    references.add(mapResultSetToReference(rs));
                }
            }
            
            return references;
            
        } catch (SQLException e) {
            throw new RepositoryException("Database error finding source references for answer: " + answerId, e);
        }
    }

    @Override
    public Optional<AnswerSourceReference> findById(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be null or blank");
        }
        
        String sql = """
            SELECT id, answer_id, document_id, chunk_id, snippet_content, snippet_context,
                   start_position, end_position, relevance_score, source_order,
                   document_title, document_filename, document_file_type,
                   page_number, chunk_index, created_at
            FROM answer_source_references
            WHERE id = ?
            """;
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToReference(rs));
                }
                return Optional.empty();
            }
            
        } catch (SQLException e) {
            throw new RepositoryException("Database error finding source reference by id: " + id, e);
        }
    }

    @Override
    public void deleteByAnswerId(String answerId) {
        if (answerId == null || answerId.isBlank()) {
            throw new IllegalArgumentException("answerId must not be null or blank");
        }
        
        String sql = "DELETE FROM answer_source_references WHERE answer_id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, answerId);
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            throw new RepositoryException("Database error deleting source references for answer: " + answerId, e);
        }
    }

    @Override
    public boolean existsByAnswerId(String answerId) {
        if (answerId == null || answerId.isBlank()) {
            throw new IllegalArgumentException("answerId must not be null or blank");
        }
        
        String sql = "SELECT 1 FROM answer_source_references WHERE answer_id = ? LIMIT 1";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, answerId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
            
        } catch (SQLException e) {
            throw new RepositoryException("Database error checking existence of source references for answer: " + answerId, e);
        }
    }

    @Override
    public long countByAnswerId(String answerId) {
        if (answerId == null || answerId.isBlank()) {
            throw new IllegalArgumentException("answerId must not be null or blank");
        }
        
        String sql = "SELECT COUNT(*) FROM answer_source_references WHERE answer_id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, answerId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
                return 0;
            }
            
        } catch (SQLException e) {
            throw new RepositoryException("Database error counting source references for answer: " + answerId, e);
        }
    }

    @Override
    public List<AnswerSourceReference> findByDocumentId(String documentId) {
        if (documentId == null || documentId.isBlank()) {
            throw new IllegalArgumentException("documentId must not be null or blank");
        }
        
        String sql = """
            SELECT id, answer_id, document_id, chunk_id, snippet_content, snippet_context,
                   start_position, end_position, relevance_score, source_order,
                   document_title, document_filename, document_file_type,
                   page_number, chunk_index, created_at
            FROM answer_source_references
            WHERE document_id = ?
            ORDER BY created_at DESC
            """;
        
        List<AnswerSourceReference> references = new ArrayList<>();
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, documentId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    references.add(mapResultSetToReference(rs));
                }
            }
            
            return references;
            
        } catch (SQLException e) {
            throw new RepositoryException("Database error finding source references for document: " + documentId, e);
        }
    }

    @Override
    public void nullifyDocumentReferences(String documentId) {
        if (documentId == null || documentId.isBlank()) {
            throw new IllegalArgumentException("documentId must not be null or blank");
        }
        
        String sql = "UPDATE answer_source_references SET document_id = NULL WHERE document_id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, documentId);
            int rowsAffected = stmt.executeUpdate();
            
            // Log the number of affected rows for monitoring
            if (rowsAffected > 0) {
                System.out.println("Nullified document references for " + rowsAffected + " source references");
            }
            
        } catch (SQLException e) {
            throw new RepositoryException("Database error nullifying document references for document: " + documentId, e);
        }
    }

    /**
     * Maps a ResultSet row to an AnswerSourceReference entity.
     */
    private AnswerSourceReference mapResultSetToReference(ResultSet rs) throws SQLException {
        return new AnswerSourceReference(
                rs.getString("id"),
                rs.getString("answer_id"),
                rs.getString("document_id"),
                rs.getString("chunk_id"),
                rs.getString("snippet_content"),
                rs.getString("snippet_context"),
                getIntegerOrNull(rs, "start_position"),
                getIntegerOrNull(rs, "end_position"),
                rs.getDouble("relevance_score"),
                rs.getInt("source_order"),
                rs.getString("document_title"),
                rs.getString("document_filename"),
                rs.getString("document_file_type"),
                getIntegerOrNull(rs, "page_number"),
                getIntegerOrNull(rs, "chunk_index"),
                rs.getTimestamp("created_at").toInstant()
        );
    }

    /**
     * Sets an integer parameter or NULL if the value is null.
     */
    private void setIntegerOrNull(PreparedStatement stmt, int index, Integer value) throws SQLException {
        if (value != null) {
            stmt.setInt(index, value);
        } else {
            stmt.setNull(index, Types.INTEGER);
        }
    }

    /**
     * Gets an integer value from ResultSet, returning null if the column is NULL.
     */
    private Integer getIntegerOrNull(ResultSet rs, String columnName) throws SQLException {
        int value = rs.getInt(columnName);
        return rs.wasNull() ? null : value;
    }

    public DataSource dataSource() {
        return dataSource;
    }
}