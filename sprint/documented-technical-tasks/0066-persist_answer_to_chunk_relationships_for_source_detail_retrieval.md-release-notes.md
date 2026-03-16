## Summary
Persisted answer-to-chunk source references in durable storage so answer detail lookups now return real snippet content after chat answers are saved, even across fresh controller instances and without relying on in-memory chunk state.

## Changes
- `backend/src/main/java/com/rag/app/domain/valueobjects/AnswerSourceReference.java`
- `backend/src/main/java/com/rag/app/usecases/interfaces/AnswerPersistence.java`
- `backend/src/main/java/com/rag/app/usecases/repositories/AnswerSourceReferenceRepository.java`
- `backend/src/main/java/com/rag/app/infrastructure/persistence/JdbcAnswerPersistence.java`
- `backend/src/main/java/com/rag/app/infrastructure/persistence/JdbcAnswerSourceReferenceRepository.java`
- `backend/src/main/java/com/rag/app/api/ChatController.java`
- `backend/src/main/java/com/rag/app/usecases/GetAnswerSourceDetails.java`
- `backend/src/main/java/com/rag/app/usecases/models/QueryDocumentsOutput.java`
- `backend/src/main/resources/schema.sql`
- `backend/src/test/java/com/rag/app/api/ChatControllerTest.java`
- `backend/src/test/java/com/rag/app/api/AnswerSourceControllerTest.java`
- `backend/src/test/java/com/rag/app/usecases/GetAnswerSourceDetailsTest.java`
- `backend/src/test/java/com/rag/app/infrastructure/persistence/JdbcAnswerSourceReferenceRepositoryTest.java`
- `backend/src/test/java/integration/ChatAnswerSourcePersistenceIntegrationTest.java`

## Impact
New chat answers are now only returned when both the answer record and its chunk-backed source references are persisted successfully, eliminating the previous mismatch where source lookups could fail with `answer not found` or empty snippet states for newly generated answers.

## Verification
- `./gradlew :backend:test :backend:verifyBuild`
- `./gradlew :backend:test :backend:integrationTest :backend:verifyBuild`

## Follow-ups
- Persist richer source context and document file-type metadata if snippet highlighting or document viewers need more precise offsets later.
- Consider migrating older answers without persisted source references if historical source detail support is needed.
