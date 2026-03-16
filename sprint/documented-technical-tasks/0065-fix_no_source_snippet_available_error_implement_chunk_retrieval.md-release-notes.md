## Summary
Fixed answer source detail retrieval so the API returns the actual retrieved chunk text when available, instead of falling back to empty snippets that produce the “No source snippet is available” message.

## Changes
- `backend/src/main/java/com/rag/app/usecases/interfaces/AnswerSourceChunkStore.java`
- `backend/src/main/java/com/rag/app/infrastructure/vector/InMemoryAnswerSourceChunkStore.java`
- `backend/src/main/java/com/rag/app/usecases/models/QueryDocumentsOutput.java`
- `backend/src/main/java/com/rag/app/usecases/QueryDocuments.java`
- `backend/src/main/java/com/rag/app/api/ChatController.java`
- `backend/src/main/java/com/rag/app/usecases/GetAnswerSourceDetails.java`
- `backend/src/test/java/com/rag/app/usecases/GetAnswerSourceDetailsTest.java`
- `backend/src/test/java/com/rag/app/api/AnswerSourceControllerTest.java`

## Impact
Fresh chat answers now preserve answer-scoped chunk relationships in memory, allowing source detail requests to resolve the exact chunk used for answer generation and to fall back more gracefully when stored reference labels do not match chunk metadata exactly.

## Verification
- `./gradlew :backend:test :backend:verifyBuild`
- `./gradlew :backend:test :backend:integrationTest :backend:verifyBuild`

## Follow-ups
- Persist answer-to-chunk relationships beyond process memory if source details must survive backend restarts.
- Consider exposing explicit debug metadata in the API when chunk matching falls back from exact reference resolution.
