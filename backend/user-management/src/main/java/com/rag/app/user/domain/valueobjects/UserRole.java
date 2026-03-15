package com.rag.app.user.domain.valueobjects;

import java.util.Set;

public enum UserRole {
    STANDARD("standard", Set.of("read_own_documents", "upload_documents", "query_own_documents")),
    ADMIN("admin", Set.of("read_all_documents", "upload_documents", "query_all_documents", "view_admin_progress", "manage_users"));

    private final String roleName;
    private final Set<String> permissions;

    UserRole(String roleName, Set<String> permissions) {
        this.roleName = roleName;
        this.permissions = permissions;
    }

    public String roleName() {
        return roleName;
    }

    public boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }
}
