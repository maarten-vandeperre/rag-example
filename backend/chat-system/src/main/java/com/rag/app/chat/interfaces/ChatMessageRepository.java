package com.rag.app.chat.interfaces;

import com.rag.app.chat.domain.entities.ChatMessage;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatMessageRepository {
    ChatMessage save(ChatMessage message);

    Optional<ChatMessage> findById(UUID messageId);

    List<ChatMessage> findByUserId(String userId);

    List<ChatMessage> findRecentByUserId(String userId, int limit);
}
