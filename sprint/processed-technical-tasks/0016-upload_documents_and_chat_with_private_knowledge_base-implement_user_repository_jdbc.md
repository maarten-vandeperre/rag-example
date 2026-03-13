# Implement User repository with JDBC

## Related User Story

User Story: upload_documents_and_chat_with_private_knowledge_base

## Objective

Implement the UserRepository interface using JDBC for user persistence and role-based access control validation.

## Scope

- Implement UserRepository interface using JDBC
- Create database schema for users table
- Implement user CRUD operations with explicit SQL
- Add user role validation methods

## Out of Scope

- User authentication mechanisms
- Password hashing and storage
- User registration workflows
- Session management

## Clean Architecture Placement

infrastructure

## Execution Dependencies

- 0002-upload_documents_and_chat_with_private_knowledge_base-create_user_domain_entity.md

## Implementation Details

Create users table schema:
```sql
CREATE TABLE users (
    user_id VARCHAR(255) PRIMARY KEY,
    username VARCHAR(100) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    role VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true
);
```

Create UserRepository interface with methods:
- save(User user)
- findById(String userId)
- findByUsername(String username)
- findByEmail(String email)
- isAdmin(String userId)
- isActiveUser(String userId)

Implement JdbcUserRepository with:
- Explicit SQL statements for all operations
- Proper SQLException handling
- User entity to database row mapping
- Role-based query methods

## Files / Modules Impacted

- backend/usecases/repositories/UserRepository.java
- backend/infrastructure/persistence/JdbcUserRepository.java
- backend/infrastructure/persistence/UserRowMapper.java
- backend/infrastructure/database/schema.sql (extend existing)

## Acceptance Criteria

Given a User entity is saved
When save() method is called
Then the user should be persisted to the database

Given a user exists with admin role
When isAdmin() is called with the user ID
Then true should be returned

Given a user exists with standard role
When isAdmin() is called with the user ID
Then false should be returned

Given an inactive user exists
When isActiveUser() is called with the user ID
Then false should be returned

## Testing Requirements

- Unit tests for JdbcUserRepository
- Integration tests with in-memory database
- Tests for role validation methods
- Tests for user lookup methods
- Tests for error handling

## Dependencies / Preconditions

- User entity must exist
- Database connection configuration must be available