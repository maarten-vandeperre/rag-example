package com.rag.app.user;

import com.rag.app.user.domain.entities.User;
import com.rag.app.user.domain.services.AuthorizationService;
import com.rag.app.user.domain.services.UserDomainService;
import com.rag.app.user.domain.valueobjects.UserId;
import com.rag.app.user.domain.valueobjects.UserRole;
import com.rag.app.user.infrastructure.UserManagementFacadeImpl;
import com.rag.app.user.infrastructure.auth.SimpleAuthenticationProvider;
import com.rag.app.user.infrastructure.session.InMemorySessionManager;
import com.rag.app.user.interfaces.SessionManager;
import com.rag.app.user.interfaces.UserRepository;
import com.rag.app.user.usecases.AuthenticateUser;
import com.rag.app.user.usecases.AuthorizeUserAction;
import com.rag.app.user.usecases.GetUserProfile;
import com.rag.app.user.usecases.ManageUserRoles;
import com.rag.app.user.usecases.models.AuthenticationRequest;
import com.rag.app.user.usecases.models.GetUserProfileInput;
import com.rag.app.user.usecases.models.ManageUserRolesInput;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserManagementFacadeTest {
    @Test
    void shouldAuthenticateAuthorizeAndManageRolesThroughFacade() {
        InMemoryUserRepository repository = new InMemoryUserRepository();
        User admin = new User(new UserId(java.util.UUID.randomUUID()), "admin", "admin@example.com", UserRole.ADMIN,
            Instant.parse("2026-03-14T10:00:00Z"), true);
        User standard = new User(new UserId(java.util.UUID.randomUUID()), "jane", "jane@example.com", UserRole.STANDARD,
            Instant.parse("2026-03-14T10:05:00Z"), true);
        repository.save(admin);
        repository.save(standard);
        SessionManager sessionManager = new InMemorySessionManager();

        UserManagementFacadeImpl facade = new UserManagementFacadeImpl(
            new AuthenticateUser(new SimpleAuthenticationProvider(repository), sessionManager, new UserDomainService()),
            new AuthorizeUserAction(repository, new AuthorizationService()),
            new GetUserProfile(repository),
            new ManageUserRoles(repository, new UserDomainService()),
            repository,
            sessionManager
        );

        var authentication = facade.authenticateUser(new AuthenticationRequest("jane", "password-for-jane"));
        assertTrue(authentication.authenticated());
        assertNotNull(authentication.sessionToken());

        var profile = facade.getUserProfile(new GetUserProfileInput(standard.userId()));
        assertEquals("jane", profile.username());

        assertTrue(facade.isAuthorized(admin.userId(), standard.userId().toString(), "manage_users"));
        assertFalse(facade.isAuthorized(standard.userId(), admin.userId().toString(), "manage_users"));

        var updated = facade.manageUserRoles(new ManageUserRolesInput(admin.userId(), standard.userId(), UserRole.ADMIN));
        assertEquals(UserRole.ADMIN, updated.assignedRole());
        assertEquals(UserRole.ADMIN, facade.getUserRole(standard.userId()));

        assertTrue(facade.isActiveUser(standard.userId()));
        assertEquals(2, facade.getAllUsers().size());

        facade.invalidateSession(authentication.sessionToken());
        assertTrue(((InMemorySessionManager) sessionManager).findUserBySession(authentication.sessionToken()).isEmpty());
    }

    private static final class InMemoryUserRepository implements UserRepository {
        private final Map<UserId, User> users = new ConcurrentHashMap<>();

        @Override
        public User save(User user) {
            users.put(user.userId(), user);
            return user;
        }

        @Override
        public Optional<User> findById(UserId userId) {
            return Optional.ofNullable(users.get(userId));
        }

        @Override
        public Optional<User> findByUsername(String username) {
            return users.values().stream().filter(user -> user.username().equals(username)).findFirst();
        }

        @Override
        public Optional<User> findByEmail(String email) {
            return users.values().stream().filter(user -> user.email().equals(email)).findFirst();
        }

        @Override
        public List<User> findAll() {
            return users.values().stream().toList();
        }

        @Override
        public boolean isAdmin(UserId userId) {
            return findById(userId).map(user -> user.role() == UserRole.ADMIN).orElse(false);
        }

        @Override
        public boolean isActiveUser(UserId userId) {
            return findById(userId).map(User::isActive).orElse(false);
        }
    }
}
