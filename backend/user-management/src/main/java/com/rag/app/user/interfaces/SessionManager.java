package com.rag.app.user.interfaces;

import com.rag.app.user.domain.entities.User;

import java.util.Optional;

public interface SessionManager {
    String createSession(User user);

    Optional<User> findUserBySession(String sessionToken);

    void invalidate(String sessionToken);
}
