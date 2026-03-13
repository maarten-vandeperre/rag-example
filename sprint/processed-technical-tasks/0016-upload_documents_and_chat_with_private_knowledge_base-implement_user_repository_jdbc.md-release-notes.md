## Summary
Implemented a JDBC-backed `UserRepository` with SQL-based persistence, lookups, and role checks, plus schema support and in-memory database coverage for repository behavior.

## Changes
Created `backend/src/main/java/com/rag/app/infrastructure/persistence/JdbcUserRepository.java`, `backend/src/main/java/com/rag/app/infrastructure/persistence/UserRowMapper.java`, and `backend/src/test/java/com/rag/app/infrastructure/persistence/JdbcUserRepositoryTest.java`.
Extended `backend/src/main/java/com/rag/app/usecases/repositories/UserRepository.java`, updated `backend/src/main/resources/schema.sql`, added the H2 test dependency in `backend/pom.xml`, and aligned supporting repository/test code in `backend/src/main/java/com/rag/app/infrastructure/persistence/JdbcChatMessageRepository.java`, `backend/src/main/java/com/rag/app/infrastructure/persistence/DocumentRowMapper.java`, `backend/src/test/java/com/rag/app/usecases/UploadDocumentTest.java`, `backend/src/test/java/com/rag/app/usecases/ProcessDocumentTest.java`, `backend/src/test/java/com/rag/app/usecases/GetUserDocumentsTest.java`, `backend/src/test/java/com/rag/app/usecases/GetAdminProgressTest.java`, and `backend/src/test/java/com/rag/app/usecases/QueryDocumentsTest.java`.

## Impact
The backend now has a concrete JDBC implementation for user persistence and role validation, and the shared schema keeps user and chat/document persistence behavior verifiable through the repository test suite.

## Verification
Executed `mvn -gs maven-settings.xml -s maven-settings.xml test` in `backend/`.

## Follow-ups
Wire `JdbcUserRepository` into runtime configuration where application services need real database-backed user lookups.
