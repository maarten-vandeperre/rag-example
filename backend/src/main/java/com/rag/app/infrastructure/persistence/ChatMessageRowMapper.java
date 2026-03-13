package com.rag.app.infrastructure.persistence;

import com.rag.app.domain.entities.ChatMessage;
import com.rag.app.domain.valueobjects.DocumentReference;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public final class ChatMessageRowMapper {

    public ChatMessage map(ResultSet resultSet, List<DocumentReference> documentReferences) throws SQLException {
        return new ChatMessage(
            UUID.fromString(resultSet.getString("message_id")),
            UUID.fromString(resultSet.getString("user_id")),
            resultSet.getString("question"),
            resultSet.getString("answer"),
            documentReferences,
            resultSet.getTimestamp("created_at").toInstant(),
            resultSet.getLong("response_time_ms")
        );
    }
}
