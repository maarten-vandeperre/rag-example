package com.rag.app.chat.domain.services;

import com.rag.app.chat.domain.entities.ChatMessage;
import com.rag.app.chat.domain.valueobjects.UserRole;

import java.time.Duration;

public final class ChatDomainService {
    public void ensureActiveUser(boolean activeUser) {
        if (!activeUser) {
            throw new IllegalArgumentException("user must be active");
        }
    }

    public void ensureValidQuestion(String question) {
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("question must not be null or empty");
        }
    }

    public boolean canSearchAllDocuments(UserRole role) {
        return role == UserRole.ADMIN;
    }

    public boolean isWithinLimit(ChatMessage message, int maxResponseTimeMs) {
        return message.isWithinResponseTimeLimit(Duration.ofMillis(maxResponseTimeMs));
    }
}
