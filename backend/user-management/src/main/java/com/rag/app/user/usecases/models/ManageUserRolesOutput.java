package com.rag.app.user.usecases.models;

import com.rag.app.user.domain.valueobjects.UserId;
import com.rag.app.user.domain.valueobjects.UserRole;

public record ManageUserRolesOutput(UserId targetUserId, UserRole assignedRole, String message) {
}
