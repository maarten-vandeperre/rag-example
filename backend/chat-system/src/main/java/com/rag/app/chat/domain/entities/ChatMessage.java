package com.rag.app.chat.domain.entities;

import com.rag.app.chat.domain.valueobjects.DocumentReference;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class ChatMessage {
    private final UUID messageId;
    private final String userId;
    private final String question;
    private final String answer;
    private final List<DocumentReference> documentReferences;
    private final Instant createdAt;
    private final Duration responseTime;

    public ChatMessage(UUID messageId,
                       String userId,
                       String question,
                       String answer,
                       List<DocumentReference> documentReferences,
                       Instant createdAt,
                       Duration responseTime) {
        this.messageId = Objects.requireNonNull(messageId, "messageId must not be null");
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId must not be null or empty");
        }
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("question must not be null or empty");
        }
        this.userId = userId;
        this.question = question;
        this.answer = answer;
        this.documentReferences = List.copyOf(Objects.requireNonNull(documentReferences, "documentReferences must not be null"));
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.responseTime = Objects.requireNonNull(responseTime, "responseTime must not be null");
        if (responseTime.isNegative() || responseTime.isZero()) {
            throw new IllegalArgumentException("responseTime must be positive");
        }
    }

    public UUID messageId() {
        return messageId;
    }

    public String userId() {
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

    public Duration responseTime() {
        return responseTime;
    }

    public boolean isAnswered() {
        return answer != null && !answer.isBlank();
    }

    public boolean hasDocumentReferences() {
        return !documentReferences.isEmpty();
    }

    public boolean isWithinResponseTimeLimit(Duration limit) {
        return responseTime.compareTo(limit) <= 0;
    }
}
