package com.rag.app.user.interfaces;

import com.rag.app.user.domain.entities.User;
import com.rag.app.user.domain.valueobjects.UserId;
import com.rag.app.user.domain.valueobjects.UserRole;
import com.rag.app.user.usecases.models.AuthenticationRequest;
import com.rag.app.user.usecases.models.AuthenticationResult;
import com.rag.app.user.usecases.models.GetUserProfileInput;
import com.rag.app.user.usecases.models.GetUserProfileOutput;
import com.rag.app.user.usecases.models.ManageUserRolesInput;
import com.rag.app.user.usecases.models.ManageUserRolesOutput;

import java.util.List;
import java.util.Optional;

public interface UserManagementFacade {
    AuthenticationResult authenticateUser(AuthenticationRequest request);

    void invalidateSession(String sessionToken);

    boolean isAuthorized(UserId userId, String resource, String action);

    UserRole getUserRole(UserId userId);

    GetUserProfileOutput getUserProfile(GetUserProfileInput input);

    Optional<User> findUserById(UserId userId);

    boolean isActiveUser(UserId userId);

    ManageUserRolesOutput manageUserRoles(ManageUserRolesInput input);

    List<User> getAllUsers();
}
