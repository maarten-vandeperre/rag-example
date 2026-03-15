package com.rag.app.integration.events.events;

import com.rag.app.shared.domain.events.DomainEvent;
import com.rag.app.user.domain.valueobjects.UserRole;

public final class UserAuthenticatedEvent extends DomainEvent {
    private final String userId;
    private final UserRole role;

    public UserAuthenticatedEvent(String userId, UserRole role) {
        super("UserAuthenticated");
        this.userId = userId;
        this.role = role;
    }

    public String userId() {
        return userId;
    }

    public UserRole role() {
        return role;
    }
}
