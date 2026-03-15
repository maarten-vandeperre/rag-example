package com.rag.app.infrastructure.persistence;

import com.rag.app.domain.entities.User;
import com.rag.app.usecases.repositories.UserRepository;

import javax.sql.DataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class JdbcUserRepository implements UserRepository {
    private static final String INSERT_SQL = "INSERT INTO users (user_id, username, email, role, created_at, is_active) VALUES (?, ?, ?, ?, ?, ?)";
    private static final String UPDATE_SQL = "UPDATE users SET username = ?, email = ?, role = ?, created_at = ?, is_active = ? WHERE user_id = ?";
    private static final String FIND_BY_ID_SQL = "SELECT user_id, username, email, role, created_at, is_active FROM users WHERE user_id = ?";
    private static final String FIND_BY_USERNAME_SQL = "SELECT user_id, username, email, role, created_at, is_active FROM users WHERE username = ?";
    private static final String FIND_BY_EMAIL_SQL = "SELECT user_id, username, email, role, created_at, is_active FROM users WHERE email = ?";
    private static final String IS_ADMIN_SQL = "SELECT role FROM users WHERE user_id = ?";
    private static final String IS_ACTIVE_SQL = "SELECT is_active FROM users WHERE user_id = ?";

    private final DataSource dataSource;
    private final UserRowMapper userRowMapper;

    @Inject
    public JdbcUserRepository(DataSource dataSource, UserRowMapper userRowMapper) {
        this.dataSource = dataSource;
        this.userRowMapper = userRowMapper;
    }

    @Override
    public User save(User user) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement updateStatement = connection.prepareStatement(UPDATE_SQL)) {
            updateStatement.setString(1, user.username());
            updateStatement.setString(2, user.email());
            updateStatement.setString(3, user.role().name());
            updateStatement.setTimestamp(4, Timestamp.from(user.createdAt()));
            updateStatement.setBoolean(5, user.isActive());
            updateStatement.setString(6, user.userId().toString());

            if (updateStatement.executeUpdate() == 0) {
                try (PreparedStatement insertStatement = connection.prepareStatement(INSERT_SQL)) {
                    insertStatement.setString(1, user.userId().toString());
                    insertStatement.setString(2, user.username());
                    insertStatement.setString(3, user.email());
                    insertStatement.setString(4, user.role().name());
                    insertStatement.setTimestamp(5, Timestamp.from(user.createdAt()));
                    insertStatement.setBoolean(6, user.isActive());
                    insertStatement.executeUpdate();
                }
            }

            return user;
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to save user", exception);
        }
    }

    @Override
    public Optional<User> findById(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId must not be null");
        }

        return findSingle(FIND_BY_ID_SQL, statement -> statement.setString(1, userId.toString()));
    }

    @Override
    public Optional<User> findByUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username must not be null or empty");
        }

        return findSingle(FIND_BY_USERNAME_SQL, statement -> statement.setString(1, username));
    }

    @Override
    public Optional<User> findByEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email must not be null or empty");
        }

        return findSingle(FIND_BY_EMAIL_SQL, statement -> statement.setString(1, email));
    }

    @Override
    public boolean isAdmin(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId must not be null");
        }

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(IS_ADMIN_SQL)) {
            statement.setString(1, userId.toString());

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && "ADMIN".equals(resultSet.getString("role"));
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to check admin role", exception);
        }
    }

    @Override
    public boolean isActiveUser(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId must not be null");
        }

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(IS_ACTIVE_SQL)) {
            statement.setString(1, userId.toString());

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getBoolean("is_active");
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to check active user state", exception);
        }
    }

    private Optional<User> findSingle(String sql, StatementBinder binder) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            binder.bind(statement);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }

                return Optional.of(userRowMapper.map(resultSet));
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to query user", exception);
        }
    }

    @FunctionalInterface
    private interface StatementBinder {
        void bind(PreparedStatement statement) throws SQLException;
    }
}
