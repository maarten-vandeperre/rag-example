# Create User Management Module

## Related User Story

User Story: improve_workspace_maintainability_with_clearer_module_boundaries_and_java_25

## Objective

Create a dedicated user management module that encapsulates all user-related functionality including authentication, authorization, role management, and user operations with clear boundaries.

## Scope

- Create user-management module structure
- Move user-related domain entities to the module
- Move user-related use cases to the module
- Move user repositories to the module
- Define clear module interfaces for authentication and authorization

## Out of Scope

- Java 25 migration (handled separately)
- Frontend module reorganization
- External authentication providers integration
- Advanced security features

## Clean Architecture Placement

domain, usecases, infrastructure

## Execution Dependencies

- 0038-improve_workspace_maintainability_with_clearer_module_boundaries_and_java_25-analyze_current_module_structure.md

## Implementation Details

Create user-management module structure:
```
backend/user-management/
├── src/main/java/com/rag/app/user/
│   ├── domain/
│   │   ├── entities/
│   │   │   └── User.java
│   │   ├── valueobjects/
│   │   │   ├── UserRole.java
│   │   │   ├── UserId.java
│   │   │   └── UserCredentials.java
│   │   └── services/
│   │       ├── UserDomainService.java
│   │       └── AuthorizationService.java
│   ├── usecases/
│   │   ├── AuthenticateUser.java
│   │   ├── AuthorizeUserAction.java
│   │   ├── GetUserProfile.java
│   │   └── ManageUserRoles.java
│   ├── interfaces/
│   │   ├── UserRepository.java
│   │   ├── AuthenticationProvider.java
│   │   └── SessionManager.java
│   └── infrastructure/
│       ├── persistence/
│       │   └── JdbcUserRepository.java
│       ├── auth/
│       │   └── SimpleAuthenticationProvider.java
│       └── session/
│           └── InMemorySessionManager.java
├── src/test/java/
└── build.gradle
```

Module build.gradle:
```gradle
plugins {
    id 'java-library'
}

dependencies {
    // Only shared-kernel dependencies allowed
    api project(':shared-kernel')
    
    // Infrastructure dependencies
    implementation 'org.postgresql:postgresql'
    implementation 'org.springframework.security:spring-security-crypto'
    implementation 'io.jsonwebtoken:jjwt-api'
    implementation 'io.jsonwebtoken:jjwt-impl'
    
    // Test dependencies
    testImplementation 'org.junit.jupiter:junit-jupiter'
    testImplementation 'org.testcontainers:postgresql'
    testImplementation 'org.mockito:mockito-core'
}

// Enforce module boundaries
configurations.all {
    resolutionStrategy.eachDependency { details ->
        if (details.requested.group == 'com.rag.app' && 
            details.requested.name != 'shared-kernel') {
            throw new GradleException(
                "User management module cannot depend on other application modules: ${details.requested}"
            )
        }
    }
}
```

Module interface definition (UserManagementFacade.java):
```java
public interface UserManagementFacade {
    // Authentication operations
    AuthenticationResult authenticateUser(AuthenticationRequest request);
    void invalidateSession(String sessionToken);
    
    // Authorization operations
    boolean isAuthorized(UserId userId, String resource, String action);
    UserRole getUserRole(UserId userId);
    
    // User operations
    GetUserProfileOutput getUserProfile(GetUserProfileInput input);
    Optional<User> findUserById(UserId userId);
    boolean isActiveUser(UserId userId);
    
    // Admin operations
    ManageUserRolesOutput manageUserRoles(ManageUserRolesInput input);
    List<User> getAllUsers();
}
```

Domain model refinements:
```java
// User entity with clear responsibilities
public class User {
    private final UserId userId;
    private final Username username;
    private final Email email;
    private final UserRole role;
    private final Timestamp createdAt;
    private final boolean isActive;
    
    // Domain methods for user-specific logic
    public boolean canAccessAllDocuments() {
        return role.equals(UserRole.ADMIN);
    }
    
    public boolean canAccessDocument(UserId documentOwnerId) {
        return isActive && (role.equals(UserRole.ADMIN) || userId.equals(documentOwnerId));
    }
    
    public boolean canPerformAdminActions() {
        return isActive && role.equals(UserRole.ADMIN);
    }
}

// UserRole value object with permissions
public enum UserRole {
    STANDARD("standard", Set.of("read_own_documents", "upload_documents", "query_own_documents")),
    ADMIN("admin", Set.of("read_all_documents", "upload_documents", "query_all_documents", 
                         "view_admin_progress", "manage_users"));
    
    private final String roleName;
    private final Set<String> permissions;
    
    public boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }
}
```

Authorization service:
```java
@Service
public class AuthorizationService {
    
    public boolean canAccessDocument(User user, String documentOwnerId) {
        if (!user.isActive()) return false;
        
        return user.canAccessAllDocuments() || 
               user.getUserId().toString().equals(documentOwnerId);
    }
    
    public boolean canPerformAdminAction(User user, String action) {
        return user.isActive() && 
               user.canPerformAdminActions() && 
               user.getRole().hasPermission(action);
    }
    
    public boolean canQueryDocuments(User user, List<String> documentOwnerIds) {
        if (!user.isActive()) return false;
        
        if (user.canAccessAllDocuments()) return true;
        
        return documentOwnerIds.stream()
            .allMatch(ownerId -> user.getUserId().toString().equals(ownerId));
    }
}
```

Module configuration:
```java
@Configuration
@ComponentScan(basePackages = "com.rag.app.user")
public class UserManagementConfiguration {
    
    @Bean
    public UserManagementFacade userManagementFacade(
            AuthenticateUser authenticateUser,
            AuthorizeUserAction authorizeUserAction,
            GetUserProfile getUserProfile,
            ManageUserRoles manageUserRoles,
            UserRepository userRepository) {
        return new UserManagementFacadeImpl(
            authenticateUser, authorizeUserAction,
            getUserProfile, manageUserRoles,
            userRepository
        );
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

Integration interfaces for other modules:
```java
// Interface for document access authorization
public interface DocumentAccessAuthorizer {
    boolean canAccessDocument(UserId userId, String documentId);
    boolean canAccessAllDocuments(UserId userId);
    List<String> getAccessibleDocumentIds(UserId userId);
}

// Interface for chat authorization
public interface ChatAuthorizer {
    boolean canQueryDocuments(UserId userId, List<String> documentIds);
    boolean canViewChatHistory(UserId userId, String chatOwnerId);
}
```

## Files / Modules Impacted

- backend/user-management/build.gradle
- backend/user-management/src/main/java/com/rag/app/user/domain/entities/User.java
- backend/user-management/src/main/java/com/rag/app/user/usecases/AuthenticateUser.java
- backend/user-management/src/main/java/com/rag/app/user/interfaces/UserManagementFacade.java
- backend/user-management/src/main/java/com/rag/app/user/infrastructure/UserManagementFacadeImpl.java
- backend/settings.gradle (add module)

## Acceptance Criteria

Given the user management module is created
When the module is built independently
Then it should compile and test successfully without other modules

Given user functionality is moved to the module
When authentication and authorization operations are performed
Then they should work through the module facade

Given module boundaries are enforced
When attempting to add dependencies to other modules
Then the build should fail with clear error messages

Given authorization interfaces are defined
When other modules need user context
Then they should only access it through defined interfaces

## Testing Requirements

- Unit tests for all moved components
- Integration tests for module facade
- Module boundary enforcement tests
- Authentication and authorization tests
- Role-based access control tests

## Dependencies / Preconditions

- Current module structure analysis must be completed
- Shared-kernel module must be defined
- Understanding of user and security functionality
- Clean Architecture principles must be followed