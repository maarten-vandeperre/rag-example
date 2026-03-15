package com.rag.app.chat;

import com.rag.app.chat.domain.entities.ChatMessage;
import com.rag.app.chat.domain.valueobjects.DocumentReference;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatMessageTest {
    @Test
    void shouldExposeChatSpecificLogic() {
        ChatMessage message = new ChatMessage(
            UUID.randomUUID(),
            "user-1",
            "What does the guide say?",
            "It explains uploads.",
            List.of(new DocumentReference(UUID.randomUUID(), "guide.pdf", "paragraph-1", 0.95d)),
            Instant.parse("2026-03-14T12:00:00Z"),
            Duration.ofMillis(250)
        );

        assertTrue(message.isAnswered());
        assertTrue(message.hasDocumentReferences());
        assertTrue(message.isWithinResponseTimeLimit(Duration.ofSeconds(1)));
        assertFalse(message.isWithinResponseTimeLimit(Duration.ofMillis(100)));
        assertEquals(250, message.responseTime().toMillis());
    }
}
