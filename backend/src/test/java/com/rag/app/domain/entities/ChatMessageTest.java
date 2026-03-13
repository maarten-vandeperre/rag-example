package com.rag.app.domain.entities;

import com.rag.app.domain.valueobjects.DocumentReference;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ChatMessageTest {

    @Test
    void shouldCreateChatMessageWhenRequiredFieldsAreValid() {
        UUID messageId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-03-13T13:00:00Z");
        DocumentReference reference = new DocumentReference(UUID.randomUUID(), "guide.pdf", "paragraph-12", 0.92d);

        ChatMessage chatMessage = new ChatMessage(
            messageId,
            userId,
            "What does the guide say about uploads?",
            "The guide explains how to upload and process documents.",
            List.of(reference),
            createdAt,
            250L
        );

        assertEquals(messageId, chatMessage.messageId());
        assertEquals(userId, chatMessage.userId());
        assertEquals("What does the guide say about uploads?", chatMessage.question());
        assertEquals("The guide explains how to upload and process documents.", chatMessage.answer());
        assertEquals(List.of(reference), chatMessage.documentReferences());
        assertEquals(createdAt, chatMessage.createdAt());
        assertEquals(250L, chatMessage.responseTimeMs());
    }

    @Test
    void shouldRejectChatMessageWhenQuestionIsNullOrBlank() {
        IllegalArgumentException nullQuestionException = assertThrows(IllegalArgumentException.class,
            () -> new ChatMessage(UUID.randomUUID(), UUID.randomUUID(), null, "answer", List.of(), Instant.now(), 10L));
        IllegalArgumentException blankQuestionException = assertThrows(IllegalArgumentException.class,
            () -> new ChatMessage(UUID.randomUUID(), UUID.randomUUID(), "   ", "answer", List.of(), Instant.now(), 10L));

        assertEquals("question must not be null or empty", nullQuestionException.getMessage());
        assertEquals("question must not be null or empty", blankQuestionException.getMessage());
    }

    @Test
    void shouldRejectChatMessageWhenResponseTimeIsNotPositive() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> new ChatMessage(UUID.randomUUID(), UUID.randomUUID(), "question", "answer", List.of(), Instant.now(), 0L));

        assertEquals("responseTimeMs must be positive", exception.getMessage());
    }

    @Test
    void shouldRejectChatMessageWhenDocumentReferencesIsNull() {
        NullPointerException exception = assertThrows(NullPointerException.class,
            () -> new ChatMessage(UUID.randomUUID(), UUID.randomUUID(), "question", "answer", null, Instant.now(), 5L));

        assertEquals("documentReferences must not be null", exception.getMessage());
    }

    @Test
    void shouldAllowEmptyDocumentReferences() {
        ChatMessage chatMessage = new ChatMessage(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "question",
            "answer",
            List.of(),
            Instant.parse("2026-03-13T13:05:00Z"),
            1L
        );

        assertEquals(List.of(), chatMessage.documentReferences());
    }
}
