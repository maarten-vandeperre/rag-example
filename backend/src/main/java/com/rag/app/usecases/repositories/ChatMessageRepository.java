package com.rag.app.usecases.repositories;

import com.rag.app.domain.entities.ChatMessage;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatMessageRepository {
    ChatMessage save(ChatMessage message);

    Optional<ChatMessage> findById(UUID messageId);

    List<ChatMessage> findByUserId(UUID userId);

    List<ChatMessage> findRecentByUserId(UUID userId, int limit);
}
