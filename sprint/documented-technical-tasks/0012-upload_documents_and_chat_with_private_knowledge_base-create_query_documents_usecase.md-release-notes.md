## Summary
Added the query documents use case with role-based access control, semantic search and answer generation contracts, and source-aware responses.

## Changes
- `backend/src/main/java/com/rag/app/usecases/QueryDocuments.java`
- `backend/src/main/java/com/rag/app/usecases/interfaces/SemanticSearch.java`
- `backend/src/main/java/com/rag/app/usecases/interfaces/AnswerGenerator.java`
- `backend/src/main/java/com/rag/app/usecases/models/DocumentChunk.java`
- `backend/src/main/java/com/rag/app/usecases/models/QueryDocumentsInput.java`
- `backend/src/main/java/com/rag/app/usecases/models/QueryDocumentsOutput.java`
- `backend/src/test/java/com/rag/app/usecases/QueryDocumentsTest.java`

## Impact
The backend application layer can now restrict document queries by user role, search only READY documents, produce answers with source references, and fail gracefully when no matches or slow responses occur.

## Verification
- `mvn -s maven-settings.xml -U compile`
- `mvn -s maven-settings.xml -U test`

## Follow-ups
- Add concrete semantic search and answer generation adapters in the infrastructure layer.
- Persist generated chat messages once the chat history workflow is introduced.
