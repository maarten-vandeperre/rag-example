package com.rag.app.infrastructure.persistence;

import com.rag.app.domain.entities.User;
import com.rag.app.domain.valueobjects.UserRole;

import jakarta.enterprise.context.ApplicationScoped;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

@ApplicationScoped
public final class UserRowMapper {

    public User map(ResultSet resultSet) throws SQLException {
        String userId = resultSet.getString("user_id");
        String username = resultSet.getString("username");
        String email = resultSet.getString("email");
        String role = resultSet.getString("role");
        Timestamp createdAt = resultSet.getTimestamp("created_at");
        boolean active = resultSet.getBoolean("is_active");

        return new User(
            UUID.fromString(userId),
            username,
            email,
            UserRole.valueOf(role),
            createdAt.toInstant(),
            active
        );
    }
}
