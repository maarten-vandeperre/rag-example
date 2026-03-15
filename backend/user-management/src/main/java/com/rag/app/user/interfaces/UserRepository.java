package com.rag.app.user.interfaces;

import com.rag.app.user.domain.entities.User;
import com.rag.app.user.domain.valueobjects.UserId;

import java.util.List;
import java.util.Optional;

public interface UserRepository {
    User save(User user);

    Optional<User> findById(UserId userId);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    List<User> findAll();

    boolean isAdmin(UserId userId);

    boolean isActiveUser(UserId userId);
}
