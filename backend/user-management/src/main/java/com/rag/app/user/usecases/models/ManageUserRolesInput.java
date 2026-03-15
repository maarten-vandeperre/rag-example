package com.rag.app.user.usecases.models;

import com.rag.app.user.domain.valueobjects.UserId;
import com.rag.app.user.domain.valueobjects.UserRole;

public record ManageUserRolesInput(UserId actorUserId, UserId targetUserId, UserRole newRole) {
}
