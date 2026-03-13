package com.rag.app.domain.entities;

import com.rag.app.domain.valueobjects.UserRole;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserTest {

    @Test
    void shouldCreateUserWhenRequiredFieldsAreValid() {
        UUID userId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-03-13T11:30:00Z");

        User user = new User(userId, "jane.doe", "jane.doe@example.com", UserRole.ADMIN, createdAt, true);

        assertEquals(userId, user.userId());
        assertEquals("jane.doe", user.username());
        assertEquals("jane.doe@example.com", user.email());
        assertEquals(UserRole.ADMIN, user.role());
        assertEquals(createdAt, user.createdAt());
        assertTrue(user.isActive());
    }

    @Test
    void shouldRejectUserWhenUsernameIsNullOrBlank() {
        IllegalArgumentException nullUsernameException = assertThrows(IllegalArgumentException.class,
            () -> new User(UUID.randomUUID(), null, "jane.doe@example.com", UserRole.STANDARD, Instant.now(), true));
        IllegalArgumentException blankUsernameException = assertThrows(IllegalArgumentException.class,
            () -> new User(UUID.randomUUID(), "   ", "jane.doe@example.com", UserRole.STANDARD, Instant.now(), true));

        assertEquals("username must not be null or empty", nullUsernameException.getMessage());
        assertEquals("username must not be null or empty", blankUsernameException.getMessage());
    }

    @Test
    void shouldRejectUserWhenEmailIsInvalid() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> new User(UUID.randomUUID(), "jane.doe", "not-an-email", UserRole.STANDARD, Instant.now(), false));

        assertEquals("email must be a valid email address", exception.getMessage());
    }

    @Test
    void shouldRejectUserWhenRoleIsNull() {
        NullPointerException exception = assertThrows(NullPointerException.class,
            () -> new User(UUID.randomUUID(), "jane.doe", "jane.doe@example.com", null, Instant.now(), true));

        assertEquals("role must not be null", exception.getMessage());
    }

    @Test
    void shouldRejectUserWhenCreatedAtIsNull() {
        NullPointerException exception = assertThrows(NullPointerException.class,
            () -> new User(UUID.randomUUID(), "jane.doe", "jane.doe@example.com", UserRole.STANDARD, null, true));

        assertEquals("createdAt must not be null", exception.getMessage());
    }

    @Test
    void shouldExposeInactiveUsers() {
        User user = new User(UUID.randomUUID(), "john.doe", "john.doe@example.com", UserRole.STANDARD,
            Instant.parse("2026-03-13T12:00:00Z"), false);

        assertFalse(user.isActive());
    }
}
