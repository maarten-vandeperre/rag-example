package com.rag.app.chat.infrastructure.persistence;

import com.rag.app.chat.domain.entities.ChatMessage;
import com.rag.app.chat.domain.exceptions.RepositoryException;
import com.rag.app.chat.domain.valueobjects.DocumentReference;
import com.rag.app.chat.interfaces.ChatMessageRepository;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

public final class JdbcChatMessageRepository implements ChatMessageRepository {
    private final DataSource dataSource;

    public JdbcChatMessageRepository(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
    }

    @Override
    public ChatMessage save(ChatMessage message) {
        Objects.requireNonNull(message, "message must not be null");
        
        String insertMessageSql = """
            INSERT INTO chat_messages (message_id, user_id, question, answer, created_at, response_time_ms)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
        
        String insertReferenceSql = """
            INSERT INTO document_references (reference_id, message_id, reference_index, document_id, document_name, paragraph_reference, relevance_score)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
        
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            
            try {
                // Insert chat message
                try (PreparedStatement stmt = conn.prepareStatement(insertMessageSql)) {
                    stmt.setString(1, message.messageId().toString());
                    stmt.setString(2, message.userId());
                    stmt.setString(3, message.question());
                    stmt.setString(4, message.answer());
                    stmt.setTimestamp(5, Timestamp.from(message.createdAt()));
                    stmt.setLong(6, message.responseTime().toMillis());
                    
                    int rowsAffected = stmt.executeUpdate();
                    if (rowsAffected != 1) {
                        throw new RepositoryException("Failed to save chat message: " + message.messageId());
                    }
                }
                
                // Insert document references
                if (!message.documentReferences().isEmpty()) {
                    try (PreparedStatement stmt = conn.prepareStatement(insertReferenceSql)) {
                        for (int i = 0; i < message.documentReferences().size(); i++) {
                            DocumentReference ref = message.documentReferences().get(i);
                            stmt.setString(1, UUID.randomUUID().toString());
                            stmt.setString(2, message.messageId().toString());
                            stmt.setInt(3, i);
                            stmt.setString(4, ref.documentId().toString());
                            stmt.setString(5, ref.documentName());
                            stmt.setString(6, ref.paragraphReference());
                            stmt.setDouble(7, ref.relevanceScore());
                            stmt.addBatch();
                        }
                        stmt.executeBatch();
                    }
                }
                
                conn.commit();
                return message;
                
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
            
        } catch (SQLException e) {
            throw new RepositoryException("Database error saving chat message: " + message.messageId(), e);
        }
    }

    @Override
    public Optional<ChatMessage> findById(UUID messageId) {
        Objects.requireNonNull(messageId, "messageId must not be null");
        
        String messageSql = """
            SELECT message_id, user_id, question, answer, created_at, response_time_ms
            FROM chat_messages
            WHERE message_id = ?
            """;
        
        String referencesSql = """
            SELECT document_id, document_name, paragraph_reference, relevance_score
            FROM document_references
            WHERE message_id = ?
            ORDER BY reference_index ASC
            """;
        
        try (Connection conn = dataSource.getConnection()) {
            
            // Find the message
            ChatMessage message = null;
            try (PreparedStatement stmt = conn.prepareStatement(messageSql)) {
                stmt.setString(1, messageId.toString());
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        // Get document references
                        List<DocumentReference> references = new ArrayList<>();
                        try (PreparedStatement refStmt = conn.prepareStatement(referencesSql)) {
                            refStmt.setString(1, messageId.toString());
                            
                            try (ResultSet refRs = refStmt.executeQuery()) {
                                while (refRs.next()) {
                                    references.add(new DocumentReference(
                                            UUID.fromString(refRs.getString("document_id")),
                                            refRs.getString("document_name"),
                                            refRs.getString("paragraph_reference"),
                                            refRs.getDouble("relevance_score")
                                    ));
                                }
                            }
                        }
                        
                        message = new ChatMessage(
                                UUID.fromString(rs.getString("message_id")),
                                rs.getString("user_id"),
                                rs.getString("question"),
                                rs.getString("answer"),
                                references,
                                rs.getTimestamp("created_at").toInstant(),
                                Duration.ofMillis(rs.getLong("response_time_ms"))
                        );
                    }
                }
            }
            
            return Optional.ofNullable(message);
            
        } catch (SQLException e) {
            throw new RepositoryException("Database error finding chat message: " + messageId, e);
        }
    }

    @Override
    public List<ChatMessage> findByUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId must not be null or blank");
        }
        
        String sql = """
            SELECT message_id, user_id, question, answer, created_at, response_time_ms
            FROM chat_messages
            WHERE user_id = ?
            ORDER BY created_at DESC
            """;
        
        return findMessagesWithReferences(sql, userId);
    }

    @Override
    public List<ChatMessage> findRecentByUserId(String userId, int limit) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId must not be null or blank");
        }
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be positive");
        }
        
        String sql = """
            SELECT message_id, user_id, question, answer, created_at, response_time_ms
            FROM chat_messages
            WHERE user_id = ?
            ORDER BY created_at DESC
            LIMIT ?
            """;
        
        return findMessagesWithReferences(sql, userId, limit);
    }
    
    private List<ChatMessage> findMessagesWithReferences(String sql, Object... params) {
        List<ChatMessage> messages = new ArrayList<>();
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            for (int i = 0; i < params.length; i++) {
                if (params[i] instanceof String) {
                    stmt.setString(i + 1, (String) params[i]);
                } else if (params[i] instanceof Integer) {
                    stmt.setInt(i + 1, (Integer) params[i]);
                }
            }
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    UUID messageId = UUID.fromString(rs.getString("message_id"));
                    
                    // Get document references for this message
                    List<DocumentReference> references = findDocumentReferences(conn, messageId);
                    
                    ChatMessage message = new ChatMessage(
                            messageId,
                            rs.getString("user_id"),
                            rs.getString("question"),
                            rs.getString("answer"),
                            references,
                            rs.getTimestamp("created_at").toInstant(),
                            Duration.ofMillis(rs.getLong("response_time_ms"))
                    );
                    
                    messages.add(message);
                }
            }
            
        } catch (SQLException e) {
            throw new RepositoryException("Database error finding chat messages", e);
        }
        
        return messages;
    }
    
    private List<DocumentReference> findDocumentReferences(Connection conn, UUID messageId) throws SQLException {
        String sql = """
            SELECT document_id, document_name, paragraph_reference, relevance_score
            FROM document_references
            WHERE message_id = ?
            ORDER BY reference_index ASC
            """;
        
        List<DocumentReference> references = new ArrayList<>();
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, messageId.toString());
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    references.add(new DocumentReference(
                            UUID.fromString(rs.getString("document_id")),
                            rs.getString("document_name"),
                            rs.getString("paragraph_reference"),
                            rs.getDouble("relevance_score")
                    ));
                }
            }
        }
        
        return references;
    }

    public DataSource dataSource() {
        return dataSource;
    }
}
