## Summary
Replaced the temporary knowledge-graph user management stub with a real CDI-wired `UserManagementFacadeImpl` and completed JDBC-backed persistence in the user-management module.

## Changes
- `backend/src/main/java/com/rag/app/config/KnowledgeGraphConfiguration.java`
- `backend/src/main/java/com/rag/app/config/UserManagementConfiguration.java`
- `backend/src/test/java/com/rag/app/config/UserManagementConfigurationTest.java`
- `backend/user-management/build.gradle`
- `backend/user-management/src/main/java/com/rag/app/user/infrastructure/persistence/JdbcUserRepository.java`
- `backend/user-management/src/test/java/com/rag/app/user/JdbcUserRepositoryTest.java`
- `backend/user-management/src/test/resources/schema.sql`

## Impact
Knowledge graph administration now uses real user repository, authentication, authorization, and session wiring, so admin access checks are enforced by actual user-management logic instead of permissive development stubs.

## Verification
- `./gradlew :backend:user-management:build`
- `./gradlew :backend:user-management:build`
- `./gradlew :backend:build`
- `./gradlew :backend:user-management:build :backend:build`

## Follow-ups
- Replace the in-memory session manager with persistent/session-token infrastructure when production auth is finalized.
- Add Quarkus CDI integration tests that boot the full application context for knowledge graph endpoints.
