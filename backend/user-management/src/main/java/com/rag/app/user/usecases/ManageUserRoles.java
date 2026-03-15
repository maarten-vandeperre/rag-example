package com.rag.app.user.usecases;

import com.rag.app.user.domain.services.UserDomainService;
import com.rag.app.user.interfaces.UserRepository;
import com.rag.app.user.usecases.models.ManageUserRolesInput;
import com.rag.app.user.usecases.models.ManageUserRolesOutput;

import java.util.Objects;

public final class ManageUserRoles {
    private final UserRepository userRepository;
    private final UserDomainService userDomainService;

    public ManageUserRoles(UserRepository userRepository, UserDomainService userDomainService) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
        this.userDomainService = Objects.requireNonNull(userDomainService, "userDomainService must not be null");
    }

    public ManageUserRolesOutput execute(ManageUserRolesInput input) {
        Objects.requireNonNull(input, "input must not be null");
        var actor = userRepository.findById(input.actorUserId()).orElseThrow(() -> new IllegalArgumentException("actor user must exist"));
        var target = userRepository.findById(input.targetUserId()).orElseThrow(() -> new IllegalArgumentException("target user must exist"));
        userDomainService.ensureRoleChangeAllowed(actor, target, input.newRole());
        userRepository.save(target.withRole(input.newRole()));
        return new ManageUserRolesOutput(input.targetUserId(), input.newRole(), "User role updated successfully");
    }
}
