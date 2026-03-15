package com.rag.app.shared;

import com.rag.app.shared.domain.events.DomainEvent;
import com.rag.app.shared.domain.exceptions.BusinessRuleViolationException;
import com.rag.app.shared.domain.exceptions.ValidationException;
import com.rag.app.shared.domain.valueobjects.Email;
import com.rag.app.shared.domain.valueobjects.EntityId;
import com.rag.app.shared.domain.valueobjects.FileSize;
import com.rag.app.shared.domain.valueobjects.Timestamp;
import com.rag.app.shared.utils.StringUtils;
import com.rag.app.shared.utils.Validator;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SharedKernelTest {
    @Test
    void shouldValidateSharedValueObjectsAndUtilities() {
        Email email = new Email("USER@example.com");
        FileSize fileSize = new FileSize(1024);
        Timestamp earlier = Timestamp.of(Instant.parse("2026-03-14T10:00:00Z"));
        Timestamp later = Timestamp.of(Instant.parse("2026-03-14T11:00:00Z"));
        SampleId sampleId = new SampleId(" abc-123 ");

        assertEquals("user@example.com", email.value());
        assertEquals(1024, fileSize.bytes());
        assertTrue(fileSize.isWithinLimit());
        assertTrue(earlier.isBefore(later));
        assertTrue(later.isAfter(earlier));
        assertEquals("abc-123", sampleId.value());
        assertTrue(StringUtils.isBlank("  "));
        assertEquals("hello", StringUtils.normalize(" hello "));

        Validator.requireNonNull(new Object(), "value");
        Validator.requireNonEmpty("value", "field");
        Validator.requirePositive(1, "count");
    }

    @Test
    void shouldExposeSharedExceptionsAndDomainEvents() {
        DomainEvent event = new SampleEvent("sample.created");
        BusinessRuleViolationException exception = new BusinessRuleViolationException("must_be_admin", "admin role required");

        assertNotNull(event.eventId());
        assertEquals("sample.created", event.eventType());
        assertNotNull(event.occurredAt());
        assertEquals("BUSINESS_RULE_VIOLATION", exception.errorCode());
        assertTrue(exception.getMessage().contains("must_be_admin"));
    }

    @Test
    void shouldRejectInvalidSharedInputs() {
        ValidationException invalidEmail = assertThrows(ValidationException.class, () -> new Email("bad-email"));
        ValidationException invalidId = assertThrows(ValidationException.class, () -> new SampleId("  "));
        ValidationException invalidFileSize = assertThrows(ValidationException.class, () -> new FileSize(FileSize.MAX_FILE_SIZE_BYTES + 1));
        ValidationException invalidPositive = assertThrows(ValidationException.class, () -> Validator.requirePositive(0, "count"));

        assertTrue(invalidEmail.getMessage().contains("Invalid email format"));
        assertEquals("Entity ID cannot be null or empty", invalidId.getMessage());
        assertEquals("File size exceeds maximum allowed size of 40MB", invalidFileSize.getMessage());
        assertEquals("count must be positive", invalidPositive.getMessage());
        assertFalse(StringUtils.isBlank("value"));
    }

    private static final class SampleId extends EntityId {
        private SampleId(String value) {
            super(value);
        }
    }

    private static final class SampleEvent extends DomainEvent {
        private SampleEvent(String eventType) {
            super(eventType);
        }
    }
}
