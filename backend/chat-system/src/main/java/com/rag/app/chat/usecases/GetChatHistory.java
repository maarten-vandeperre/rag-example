package com.rag.app.chat.usecases;

import com.rag.app.chat.interfaces.ChatMessageRepository;
import com.rag.app.chat.usecases.models.GetChatHistoryInput;
import com.rag.app.chat.usecases.models.GetChatHistoryOutput;

import java.util.Objects;

public final class GetChatHistory {
    private final ChatMessageRepository chatMessageRepository;

    public GetChatHistory(ChatMessageRepository chatMessageRepository) {
        this.chatMessageRepository = Objects.requireNonNull(chatMessageRepository, "chatMessageRepository must not be null");
    }

    public GetChatHistoryOutput execute(GetChatHistoryInput input) {
        Objects.requireNonNull(input, "input must not be null");
        if (input.userId() == null || input.userId().isBlank()) {
            throw new IllegalArgumentException("userId must not be null or empty");
        }
        if (input.limit() <= 0) {
            throw new IllegalArgumentException("limit must be positive");
        }
        return new GetChatHistoryOutput(chatMessageRepository.findRecentByUserId(input.userId(), input.limit()));
    }
}
