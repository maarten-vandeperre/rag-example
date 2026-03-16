## Summary
Added backend APIs for retrieving answer source details and full document content, while persisting chat answers with identifiers that downstream clients can use to request those details.

## Changes
- `backend/src/main/java/com/rag/app/api/AnswerSourceController.java`
- `backend/src/main/java/com/rag/app/api/DocumentContentController.java`
- `backend/src/main/java/com/rag/app/api/ChatController.java`
- `backend/src/main/java/com/rag/app/api/dto/ChatQueryResponse.java`
- `backend/src/main/java/com/rag/app/usecases/GetAnswerSourceDetails.java`
- `backend/src/main/java/com/rag/app/usecases/GetDocumentContent.java`
- `backend/src/main/java/com/rag/app/usecases/interfaces/DocumentChunkStore.java`
- `backend/src/main/java/com/rag/app/infrastructure/vector/VectorStoreImpl.java`
- `backend/src/main/java/com/rag/app/infrastructure/persistence/JdbcChatMessageRepository.java`
- `backend/src/test/java/com/rag/app/api/AnswerSourceControllerTest.java`
- `backend/src/test/java/com/rag/app/api/DocumentContentControllerTest.java`
- `backend/src/test/java/com/rag/app/usecases/GetAnswerSourceDetailsTest.java`

## Impact
The backend now exposes answer-scoped source metadata and document content endpoints, supports access checks for answer/document ownership, and returns stable answer IDs from chat responses for later detail lookups.

## Verification
- `./gradlew :backend:test :backend:verifyBuild`
- `./gradlew :backend:test :backend:integrationTest :backend:verifyBuild`

## Follow-ups
- Add a dedicated repository-backed content store so source and document detail APIs survive application restarts.
- Extend frontend clients to consume `answerId` and the new detail endpoints.
