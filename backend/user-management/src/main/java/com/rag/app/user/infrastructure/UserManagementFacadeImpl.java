package com.rag.app.user.infrastructure;

import com.rag.app.user.domain.entities.User;
import com.rag.app.user.domain.valueobjects.UserId;
import com.rag.app.user.domain.valueobjects.UserRole;
import com.rag.app.user.interfaces.SessionManager;
import com.rag.app.user.interfaces.UserManagementFacade;
import com.rag.app.user.interfaces.UserRepository;
import com.rag.app.user.usecases.AuthenticateUser;
import com.rag.app.user.usecases.AuthorizeUserAction;
import com.rag.app.user.usecases.GetUserProfile;
import com.rag.app.user.usecases.ManageUserRoles;
import com.rag.app.user.usecases.models.AuthenticationRequest;
import com.rag.app.user.usecases.models.AuthenticationResult;
import com.rag.app.user.usecases.models.GetUserProfileInput;
import com.rag.app.user.usecases.models.GetUserProfileOutput;
import com.rag.app.user.usecases.models.ManageUserRolesInput;
import com.rag.app.user.usecases.models.ManageUserRolesOutput;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class UserManagementFacadeImpl implements UserManagementFacade {
    private final AuthenticateUser authenticateUser;
    private final AuthorizeUserAction authorizeUserAction;
    private final GetUserProfile getUserProfile;
    private final ManageUserRoles manageUserRoles;
    private final UserRepository userRepository;
    private final SessionManager sessionManager;

    public UserManagementFacadeImpl(AuthenticateUser authenticateUser,
                                    AuthorizeUserAction authorizeUserAction,
                                    GetUserProfile getUserProfile,
                                    ManageUserRoles manageUserRoles,
                                    UserRepository userRepository,
                                    SessionManager sessionManager) {
        this.authenticateUser = Objects.requireNonNull(authenticateUser, "authenticateUser must not be null");
        this.authorizeUserAction = Objects.requireNonNull(authorizeUserAction, "authorizeUserAction must not be null");
        this.getUserProfile = Objects.requireNonNull(getUserProfile, "getUserProfile must not be null");
        this.manageUserRoles = Objects.requireNonNull(manageUserRoles, "manageUserRoles must not be null");
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
        this.sessionManager = Objects.requireNonNull(sessionManager, "sessionManager must not be null");
    }

    @Override
    public AuthenticationResult authenticateUser(AuthenticationRequest request) {
        return authenticateUser.execute(request);
    }

    @Override
    public void invalidateSession(String sessionToken) {
        sessionManager.invalidate(sessionToken);
    }

    @Override
    public boolean isAuthorized(UserId userId, String resource, String action) {
        return authorizeUserAction.execute(userId, resource, action);
    }

    @Override
    public UserRole getUserRole(UserId userId) {
        return userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("user must exist")).role();
    }

    @Override
    public GetUserProfileOutput getUserProfile(GetUserProfileInput input) {
        return getUserProfile.execute(input);
    }

    @Override
    public Optional<User> findUserById(UserId userId) {
        return userRepository.findById(userId);
    }

    @Override
    public boolean isActiveUser(UserId userId) {
        return userRepository.isActiveUser(userId);
    }

    @Override
    public ManageUserRolesOutput manageUserRoles(ManageUserRolesInput input) {
        return manageUserRoles.execute(input);
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}
