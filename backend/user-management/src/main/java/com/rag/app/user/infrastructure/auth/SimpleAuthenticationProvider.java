package com.rag.app.user.infrastructure.auth;

import com.rag.app.user.domain.entities.User;
import com.rag.app.user.domain.valueobjects.UserCredentials;
import com.rag.app.user.interfaces.AuthenticationProvider;
import com.rag.app.user.interfaces.UserRepository;

import java.util.Objects;
import java.util.Optional;

public final class SimpleAuthenticationProvider implements AuthenticationProvider {
    private final UserRepository userRepository;

    public SimpleAuthenticationProvider(UserRepository userRepository) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
    }

    @Override
    public Optional<User> authenticate(UserCredentials credentials) {
        return userRepository.findByUsername(credentials.username())
            .filter(user -> credentials.password().equals("password-for-" + user.username()));
    }
}
