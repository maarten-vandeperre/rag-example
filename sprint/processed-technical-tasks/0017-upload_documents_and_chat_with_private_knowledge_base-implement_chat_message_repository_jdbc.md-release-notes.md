## Summary
Implemented JDBC persistence for chat messages, including child document references, transactional saves, and user-scoped retrieval methods.

## Changes
- `backend/src/main/java/com/rag/app/usecases/repositories/ChatMessageRepository.java`
- `backend/src/main/java/com/rag/app/infrastructure/persistence/JdbcChatMessageRepository.java`
- `backend/src/main/java/com/rag/app/infrastructure/persistence/ChatMessageRowMapper.java`
- `backend/src/main/java/com/rag/app/infrastructure/persistence/DocumentReferenceRowMapper.java`
- `backend/src/main/resources/schema.sql`
- `backend/src/test/java/com/rag/app/infrastructure/persistence/JdbcChatMessageRepositoryTest.java`
- `backend/pom.xml`
- `backend/src/main/java/com/rag/app/infrastructure/persistence/JdbcDocumentRepository.java`
- `backend/src/test/java/com/rag/app/usecases/QueryDocumentsTest.java`

## Impact
The backend now has a concrete repository contract and JDBC adapter for storing chat history with document references, and the shared schema includes the tables and indexes needed for chat persistence.

## Verification
- `mvn -s maven-settings.xml test`
- `mvn -s maven-settings.xml compile && mvn -s maven-settings.xml test`

## Follow-ups
- Wire `JdbcChatMessageRepository` into the application layer once the chat querying workflow is exposed through API endpoints.
