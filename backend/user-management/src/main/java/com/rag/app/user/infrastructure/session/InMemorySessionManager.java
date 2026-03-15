package com.rag.app.user.infrastructure.session;

import com.rag.app.user.domain.entities.User;
import com.rag.app.user.interfaces.SessionManager;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemorySessionManager implements SessionManager {
    private final Map<String, User> sessions = new ConcurrentHashMap<>();

    @Override
    public String createSession(User user) {
        String token = UUID.randomUUID().toString();
        sessions.put(token, user);
        return token;
    }

    @Override
    public Optional<User> findUserBySession(String sessionToken) {
        return Optional.ofNullable(sessions.get(sessionToken));
    }

    @Override
    public void invalidate(String sessionToken) {
        sessions.remove(sessionToken);
    }
}
