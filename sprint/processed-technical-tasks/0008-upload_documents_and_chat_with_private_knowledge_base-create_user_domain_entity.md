# Create User domain entity

## Related User Story

User Story: upload_documents_and_chat_with_private_knowledge_base

## Objective

Create the User domain entity to represent users in the system with role-based access control for document visibility.

## Scope

- Create User domain entity with essential properties
- Include user role enumeration for standard and admin users
- Add basic validation rules

## Out of Scope

- Authentication mechanisms
- Password handling
- User registration flows
- Persistence concerns

## Clean Architecture Placement

domain

## Execution Dependencies

- 0002-upload_documents_and_chat_with_private_knowledge_base-create_postgresql_container_setup.md

## Implementation Details

Create a User entity with the following properties:
- userId (unique identifier)
- username (unique username)
- email (user email address)
- role (STANDARD, ADMIN)
- createdAt (timestamp)
- isActive (boolean flag)

Create UserRole enumeration with values:
- STANDARD (can only access own documents)
- ADMIN (can access all documents)

Validation rules:
- userId must not be null
- username must not be null or empty
- email must be valid email format
- role must not be null
- createdAt must not be null

## Files / Modules Impacted

- backend/domain/entities/User.java
- backend/domain/valueobjects/UserRole.java

## Acceptance Criteria

Given a User entity is created
When all required properties are provided with valid values
Then the entity should be successfully instantiated

Given a User entity is created with invalid email
When email format is invalid
Then validation should fail

Given a User entity is created with null username
When username is null or empty
Then validation should fail

## Testing Requirements

- Unit tests for User entity creation
- Unit tests for validation rules
- Unit tests for UserRole enumeration

## Dependencies / Preconditions

- Database schema must be defined