package com.rag.app.usecases.repositories;

import com.rag.app.domain.entities.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    User save(User user);

    Optional<User> findById(UUID userId);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean isAdmin(UUID userId);

    boolean isActiveUser(UUID userId);
}
