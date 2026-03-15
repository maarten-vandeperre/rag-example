package com.rag.app.user;

import com.rag.app.user.domain.entities.User;
import com.rag.app.user.domain.services.AuthorizationService;
import com.rag.app.user.domain.valueobjects.UserId;
import com.rag.app.user.domain.valueobjects.UserRole;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserAuthorizationTest {
    @Test
    void shouldApplyRoleBasedAccessRules() {
        AuthorizationService authorizationService = new AuthorizationService();
        User admin = new User(new UserId(UUID.randomUUID()), "admin", "admin@example.com", UserRole.ADMIN, Instant.now(), true);
        User standard = new User(new UserId(UUID.randomUUID()), "user", "user@example.com", UserRole.STANDARD, Instant.now(), true);

        assertTrue(authorizationService.canPerformAdminAction(admin, "manage_users"));
        assertFalse(authorizationService.canPerformAdminAction(standard, "manage_users"));
        assertTrue(authorizationService.canAccessDocument(admin, "someone-else"));
        assertTrue(authorizationService.canAccessDocument(standard, standard.userId().toString()));
        assertFalse(authorizationService.canAccessDocument(standard, "someone-else"));
        assertTrue(authorizationService.canQueryDocuments(admin, List.of("a", "b")));
        assertFalse(authorizationService.canQueryDocuments(standard, List.of("a", "b")));
    }
}
