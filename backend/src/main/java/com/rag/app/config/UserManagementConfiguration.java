package com.rag.app.config;

import com.rag.app.user.domain.services.AuthorizationService;
import com.rag.app.user.domain.services.UserDomainService;
import com.rag.app.user.infrastructure.UserManagementFacadeImpl;
import com.rag.app.user.infrastructure.auth.SimpleAuthenticationProvider;
import com.rag.app.user.infrastructure.persistence.JdbcUserRepository;
import com.rag.app.user.infrastructure.session.InMemorySessionManager;
import com.rag.app.user.interfaces.AuthenticationProvider;
import com.rag.app.user.interfaces.SessionManager;
import com.rag.app.user.interfaces.UserManagementFacade;
import com.rag.app.user.interfaces.UserRepository;
import com.rag.app.user.usecases.AuthenticateUser;
import com.rag.app.user.usecases.AuthorizeUserAction;
import com.rag.app.user.usecases.GetUserProfile;
import com.rag.app.user.usecases.ManageUserRoles;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import javax.sql.DataSource;

@ApplicationScoped
public class UserManagementConfiguration {
    @Produces
    @ApplicationScoped
    public UserRepository userRepository(DataSource dataSource) {
        return new JdbcUserRepository(dataSource);
    }

    @Produces
    @ApplicationScoped
    public SessionManager sessionManager() {
        return new InMemorySessionManager();
    }

    @Produces
    @ApplicationScoped
    public AuthenticationProvider authenticationProvider(UserRepository userRepository) {
        return new SimpleAuthenticationProvider(userRepository);
    }

    @Produces
    @ApplicationScoped
    public UserDomainService userDomainService() {
        return new UserDomainService();
    }

    @Produces
    @ApplicationScoped
    public AuthorizationService authorizationService() {
        return new AuthorizationService();
    }

    @Produces
    @ApplicationScoped
    public AuthenticateUser authenticateUser(AuthenticationProvider authenticationProvider,
                                             SessionManager sessionManager,
                                             UserDomainService userDomainService) {
        return new AuthenticateUser(authenticationProvider, sessionManager, userDomainService);
    }

    @Produces
    @ApplicationScoped
    public AuthorizeUserAction authorizeUserAction(UserRepository userRepository,
                                                   AuthorizationService authorizationService) {
        return new AuthorizeUserAction(userRepository, authorizationService);
    }

    @Produces
    @ApplicationScoped
    public GetUserProfile getUserProfile(UserRepository userRepository) {
        return new GetUserProfile(userRepository);
    }

    @Produces
    @ApplicationScoped
    public ManageUserRoles manageUserRoles(UserRepository userRepository,
                                           UserDomainService userDomainService) {
        return new ManageUserRoles(userRepository, userDomainService);
    }

    @Produces
    @ApplicationScoped
    public UserManagementFacade userManagementFacade(AuthenticateUser authenticateUser,
                                                     AuthorizeUserAction authorizeUserAction,
                                                     GetUserProfile getUserProfile,
                                                     ManageUserRoles manageUserRoles,
                                                     UserRepository userRepository,
                                                     SessionManager sessionManager) {
        return new UserManagementFacadeImpl(
            authenticateUser,
            authorizeUserAction,
            getUserProfile,
            manageUserRoles,
            userRepository,
            sessionManager
        );
    }
}
