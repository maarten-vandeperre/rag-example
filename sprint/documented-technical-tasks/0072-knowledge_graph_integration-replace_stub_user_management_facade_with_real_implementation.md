# Replace stub UserManagementFacade with real implementation

## Related User Story

Knowledge Graph Integration - Next Steps

## Objective

Replace the temporary stub UserManagementFacade implementation in KnowledgeGraphConfiguration with the actual UserManagementFacadeImpl to enable proper admin access control for knowledge graph endpoints.

## Scope

- Remove stub UserManagementFacade producer method
- Create proper CDI configuration for UserManagementFacadeImpl
- Wire up all required dependencies for user management
- Ensure admin access control works correctly for knowledge graph endpoints

## Out of Scope

- Modifying user management domain logic
- Creating new user management features
- Frontend user interface changes

## Clean Architecture Placement

- infrastructure (CDI configuration)
- interface adapters (dependency wiring)

## Execution Dependencies

None

## Implementation Details

### Remove Stub Implementation
- Delete the stub UserManagementFacade producer method from KnowledgeGraphConfiguration
- Remove anonymous implementation

### Create UserManagement CDI Configuration
- Create producer methods for all UserManagementFacadeImpl dependencies:
  - AuthenticateUser use case
  - AuthorizeUserAction use case  
  - GetUserProfile use case
  - ManageUserRoles use case
  - UserRepository implementation
  - SessionManager implementation
- Create producer method for UserManagementFacadeImpl

### Dependency Wiring
- Ensure all user management components are properly injected
- Verify CDI scope annotations are correct (@ApplicationScoped)
- Handle any missing repository implementations

### Validation
- Test that admin users can access knowledge graph endpoints
- Test that non-admin users are properly rejected
- Verify proper error responses for unauthorized access

## Files / Modules Impacted

- `backend/src/main/java/com/rag/app/config/KnowledgeGraphConfiguration.java`
- `backend/user-management/` module components
- Potentially new configuration class for user management

## Acceptance Criteria

**Given** a properly configured UserManagementFacade
**When** an admin user accesses knowledge graph endpoints
**Then** the request should be processed successfully

**Given** a properly configured UserManagementFacade  
**When** a non-admin user accesses knowledge graph endpoints
**Then** the request should be rejected with 403 Forbidden

**Given** the UserManagementFacade is properly wired
**When** the backend starts up
**Then** there should be no CDI dependency injection errors

## Testing Requirements

- Unit tests for CDI configuration
- Integration tests for admin access control
- Test with valid and invalid user IDs
- Test with different user roles

## Dependencies / Preconditions

- UserManagementFacadeImpl exists and is functional
- User repository implementation is available
- Session management is properly configured