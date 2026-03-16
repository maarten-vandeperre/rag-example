package com.rag.app.user;

import com.rag.app.user.domain.entities.User;
import com.rag.app.user.domain.valueobjects.UserId;
import com.rag.app.user.domain.valueobjects.UserRole;
import com.rag.app.user.infrastructure.persistence.JdbcUserRepository;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcUserRepositoryTest {
    @Test
    void shouldSaveAndFindUsersThroughJdbc() throws SQLException {
        JdbcUserRepository repository = new JdbcUserRepository(initializedDataSource());
        User user = new User(new UserId(UUID.randomUUID()), "jane.doe", "jane.doe@example.com", UserRole.ADMIN,
            Instant.parse("2026-03-13T09:00:00Z"), true);

        repository.save(user);

        assertEquals(Optional.of(user.userId()), repository.findById(user.userId()).map(User::userId));
        assertEquals(Optional.of("jane.doe"), repository.findByUsername("jane.doe").map(User::username));
        assertEquals(Optional.of("jane.doe@example.com"), repository.findByEmail("jane.doe@example.com").map(User::email));
        assertEquals(1, repository.findAll().size());
        assertTrue(repository.isAdmin(user.userId()));
        assertTrue(repository.isActiveUser(user.userId()));
    }

    @Test
    void shouldUpdateExistingUserState() throws SQLException {
        JdbcUserRepository repository = new JdbcUserRepository(initializedDataSource());
        UserId userId = new UserId(UUID.randomUUID());

        repository.save(new User(userId, "john", "john@example.com", UserRole.STANDARD,
            Instant.parse("2026-03-13T09:00:00Z"), true));
        repository.save(new User(userId, "john-admin", "john.admin@example.com", UserRole.ADMIN,
            Instant.parse("2026-03-13T10:00:00Z"), false));

        User updatedUser = repository.findById(userId).orElseThrow();

        assertEquals("john-admin", updatedUser.username());
        assertEquals("john.admin@example.com", updatedUser.email());
        assertEquals(UserRole.ADMIN, updatedUser.role());
        assertFalse(updatedUser.isActive());
        assertTrue(repository.isAdmin(userId));
        assertFalse(repository.isActiveUser(userId));
    }

    @Test
    void shouldReturnEmptyForUnknownUsers() throws SQLException {
        JdbcUserRepository repository = new JdbcUserRepository(initializedDataSource());
        UserId unknownUserId = new UserId(UUID.randomUUID());

        assertTrue(repository.findById(unknownUserId).isEmpty());
        assertTrue(repository.findByUsername("missing").isEmpty());
        assertTrue(repository.findByEmail("missing@example.com").isEmpty());
        assertFalse(repository.isAdmin(unknownUserId));
        assertFalse(repository.isActiveUser(unknownUserId));
    }

    @Test
    void shouldWrapSqlExceptions() {
        JdbcUserRepository repository = new JdbcUserRepository(new BrokenDataSource());
        User user = new User(new UserId(UUID.randomUUID()), "broken", "broken@example.com", UserRole.STANDARD,
            Instant.parse("2026-03-13T09:00:00Z"), true);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> repository.save(user));

        assertEquals("Failed to save user", exception.getMessage());
    }

    private static DataSource initializedDataSource() throws SQLException {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:user-management-" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("sa");

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(loadSchemaSql());
        }

        return dataSource;
    }

    private static String loadSchemaSql() {
        try (InputStream inputStream = JdbcUserRepositoryTest.class.getClassLoader().getResourceAsStream("schema.sql")) {
            if (inputStream == null) {
                throw new IllegalStateException("schema.sql resource not found");
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to read schema.sql", exception);
        }
    }

    private static final class BrokenDataSource implements DataSource {
        @Override
        public Connection getConnection() throws SQLException {
            throw new SQLException("connection unavailable");
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            throw new SQLException("connection unavailable");
        }

        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException {
            throw new SQLException("unwrap unsupported");
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) {
            return false;
        }

        @Override
        public PrintWriter getLogWriter() {
            return null;
        }

        @Override
        public void setLogWriter(PrintWriter out) {
        }

        @Override
        public void setLoginTimeout(int seconds) {
        }

        @Override
        public int getLoginTimeout() {
            return 0;
        }

        @Override
        public Logger getParentLogger() throws SQLFeatureNotSupportedException {
            throw new SQLFeatureNotSupportedException("No parent logger");
        }
    }
}
