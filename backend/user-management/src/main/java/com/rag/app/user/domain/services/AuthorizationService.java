package com.rag.app.user.domain.services;

import com.rag.app.user.domain.entities.User;

import java.util.List;

public final class AuthorizationService {
    public boolean canAccessDocument(User user, String documentOwnerId) {
        return user != null && user.isActive() && (user.canAccessAllDocuments() || user.userId().toString().equals(documentOwnerId));
    }

    public boolean canPerformAdminAction(User user, String action) {
        return user != null && user.isActive() && user.canPerformAdminActions() && user.role().hasPermission(action);
    }

    public boolean canQueryDocuments(User user, List<String> documentOwnerIds) {
        if (user == null || !user.isActive()) {
            return false;
        }
        if (user.canAccessAllDocuments()) {
            return true;
        }
        return documentOwnerIds.stream().allMatch(ownerId -> user.userId().toString().equals(ownerId));
    }
}
