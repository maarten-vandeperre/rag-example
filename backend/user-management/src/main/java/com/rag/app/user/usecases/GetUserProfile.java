package com.rag.app.user.usecases;

import com.rag.app.user.interfaces.UserRepository;
import com.rag.app.user.usecases.models.GetUserProfileInput;
import com.rag.app.user.usecases.models.GetUserProfileOutput;

import java.util.Objects;

public final class GetUserProfile {
    private final UserRepository userRepository;

    public GetUserProfile(UserRepository userRepository) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
    }

    public GetUserProfileOutput execute(GetUserProfileInput input) {
        Objects.requireNonNull(input, "input must not be null");
        if (input.userId() == null) {
            throw new IllegalArgumentException("userId must not be null");
        }
        var user = userRepository.findById(input.userId()).orElseThrow(() -> new IllegalArgumentException("user must exist"));
        return new GetUserProfileOutput(user.userId(), user.username(), user.email(), user.role(), user.createdAt(), user.isActive());
    }
}
