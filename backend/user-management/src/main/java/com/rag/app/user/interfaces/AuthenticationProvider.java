package com.rag.app.user.interfaces;

import com.rag.app.user.domain.entities.User;
import com.rag.app.user.domain.valueobjects.UserCredentials;

import java.util.Optional;

public interface AuthenticationProvider {
    Optional<User> authenticate(UserCredentials credentials);
}
