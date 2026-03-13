package com.rag.app.domain.entities;

import com.rag.app.domain.valueobjects.DocumentReference;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class ChatMessage {
    private final UUID messageId;
    private final UUID userId;
    private final String question;
    private final String answer;
    private final List<DocumentReference> documentReferences;
    private final Instant createdAt;
    private final long responseTimeMs;

    public ChatMessage(UUID messageId,
                       UUID userId,
                       String question,
                       String answer,
                       List<DocumentReference> documentReferences,
                       Instant createdAt,
                       long responseTimeMs) {
        this.messageId = Objects.requireNonNull(messageId, "messageId must not be null");
        this.userId = Objects.requireNonNull(userId, "userId must not be null");

        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("question must not be null or empty");
        }
        if (responseTimeMs <= 0) {
            throw new IllegalArgumentException("responseTimeMs must be positive");
        }

        this.question = question;
        this.answer = answer;
        this.documentReferences = List.copyOf(Objects.requireNonNull(documentReferences, "documentReferences must not be null"));
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.responseTimeMs = responseTimeMs;
    }

    public UUID messageId() {
        return messageId;
    }

    public UUID userId() {
        return userId;
    }

    public String question() {
        return question;
    }

    public String answer() {
        return answer;
    }

    public List<DocumentReference> documentReferences() {
        return documentReferences;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public long responseTimeMs() {
        return responseTimeMs;
    }
}
