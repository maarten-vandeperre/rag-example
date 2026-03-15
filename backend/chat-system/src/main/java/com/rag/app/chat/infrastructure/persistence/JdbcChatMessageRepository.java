package com.rag.app.chat.infrastructure.persistence;

import com.rag.app.chat.domain.entities.ChatMessage;
import com.rag.app.chat.interfaces.ChatMessageRepository;

import javax.sql.DataSource;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class JdbcChatMessageRepository implements ChatMessageRepository {
    private final DataSource dataSource;

    public JdbcChatMessageRepository(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
    }

    @Override
    public ChatMessage save(ChatMessage message) {
        throw new UnsupportedOperationException("JdbcChatMessageRepository persistence wiring is not yet connected in this module");
    }

    @Override
    public Optional<ChatMessage> findById(UUID messageId) {
        return Optional.empty();
    }

    @Override
    public List<ChatMessage> findByUserId(String userId) {
        return List.of();
    }

    @Override
    public List<ChatMessage> findRecentByUserId(String userId, int limit) {
        return List.of();
    }

    public DataSource dataSource() {
        return dataSource;
    }
}
