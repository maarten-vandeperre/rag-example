## Summary
Implemented a JDBC-backed document repository with explicit SQL for persistence, lookup queries, status updates, and admin progress reporting.

## Changes
- `backend/src/main/java/com/rag/app/infrastructure/persistence/JdbcDocumentRepository.java`
- `backend/src/main/java/com/rag/app/infrastructure/persistence/DocumentRowMapper.java`
- `backend/src/main/java/com/rag/app/usecases/repositories/DocumentRepository.java`
- `backend/src/main/resources/schema.sql`
- `backend/src/test/java/com/rag/app/infrastructure/persistence/JdbcDocumentRepositoryTest.java`
- `backend/src/test/java/com/rag/app/usecases/UploadDocumentTest.java`
- `backend/src/test/java/com/rag/app/usecases/ProcessDocumentTest.java`
- `backend/src/test/java/com/rag/app/usecases/GetUserDocumentsTest.java`
- `backend/src/test/java/com/rag/app/usecases/GetAdminProgressTest.java`

## Impact
The backend infrastructure now persists documents through JDBC and supports the repository queries required by upload, listing, processing, and admin monitoring use cases.

## Verification
- `mvn -s maven-settings.xml -U compile`
- `mvn -s maven-settings.xml -U test`

## Follow-ups
- Wire failure reasons into processing flows so failed-document reporting includes detailed causes.
- Add startup schema initialization or migration handling for non-test environments.
