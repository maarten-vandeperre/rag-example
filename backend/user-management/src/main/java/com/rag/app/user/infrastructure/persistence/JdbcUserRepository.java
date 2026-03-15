package com.rag.app.user.infrastructure.persistence;

import com.rag.app.user.domain.entities.User;
import com.rag.app.user.domain.valueobjects.UserId;
import com.rag.app.user.domain.valueobjects.UserRole;
import com.rag.app.user.interfaces.UserRepository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class JdbcUserRepository implements UserRepository {
    private final DataSource dataSource;

    public JdbcUserRepository(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
    }

    @Override
    public User save(User user) {
        throw new UnsupportedOperationException("JdbcUserRepository persistence wiring is not yet connected in this module");
    }

    @Override
    public Optional<User> findById(UserId userId) {
        return Optional.empty();
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return Optional.empty();
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return Optional.empty();
    }

    @Override
    public List<User> findAll() {
        return List.of();
    }

    @Override
    public boolean isAdmin(UserId userId) {
        return findById(userId).map(user -> user.role() == UserRole.ADMIN).orElse(false);
    }

    @Override
    public boolean isActiveUser(UserId userId) {
        return findById(userId).map(User::isActive).orElse(false);
    }

    public DataSource dataSource() {
        return dataSource;
    }

    public User mapRow(ResultSet resultSet) throws SQLException {
        return new User(
            new UserId(java.util.UUID.fromString(resultSet.getString("user_id"))),
            resultSet.getString("username"),
            resultSet.getString("email"),
            UserRole.valueOf(resultSet.getString("role")),
            resultSet.getTimestamp("created_at").toInstant(),
            resultSet.getBoolean("is_active")
        );
    }
}
