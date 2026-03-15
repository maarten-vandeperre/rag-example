package com.rag.app.user.domain.services;

import com.rag.app.user.domain.entities.User;
import com.rag.app.user.domain.valueobjects.UserRole;

public final class UserDomainService {
    public void ensureActive(User user) {
        if (!user.isActive()) {
            throw new IllegalArgumentException("user must be active");
        }
    }

    public void ensureAdmin(User user) {
        if (!user.canPerformAdminActions()) {
            throw new IllegalArgumentException("user must be an active admin");
        }
    }

    public void ensureRoleChangeAllowed(User actor, User target, UserRole newRole) {
        ensureAdmin(actor);
        if (target == null) {
            throw new IllegalArgumentException("target user must exist");
        }
        if (newRole == null) {
            throw new IllegalArgumentException("newRole must not be null");
        }
    }
}
