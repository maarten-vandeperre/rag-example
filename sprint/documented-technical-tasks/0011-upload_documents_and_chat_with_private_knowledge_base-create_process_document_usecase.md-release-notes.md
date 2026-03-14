## Summary
Added the document processing use case with status transitions, extraction and vector storage interfaces, and failure handling for unusable or errored content.

## Changes
- `backend/src/main/java/com/rag/app/usecases/ProcessDocument.java`
- `backend/src/main/java/com/rag/app/usecases/interfaces/DocumentContentExtractor.java`
- `backend/src/main/java/com/rag/app/usecases/interfaces/VectorStore.java`
- `backend/src/main/java/com/rag/app/usecases/models/ProcessDocumentInput.java`
- `backend/src/main/java/com/rag/app/usecases/models/ProcessDocumentOutput.java`
- `backend/src/main/java/com/rag/app/usecases/repositories/DocumentRepository.java`
- `backend/src/main/java/com/rag/app/domain/entities/Document.java`
- `backend/src/test/java/com/rag/app/usecases/ProcessDocumentTest.java`
- `backend/src/test/java/com/rag/app/usecases/UploadDocumentTest.java`

## Impact
The backend application layer can now move uploaded documents through processing, mark them ready when extraction and vector storage succeed, and fail safely when content or downstream steps break.

## Verification
- `mvn -s maven-settings.xml -U compile`
- `mvn -s maven-settings.xml -U test`

## Follow-ups
- Add persisted processing error details if operators need more visibility into failed documents.
- Replace byte-array input with stored file retrieval once infrastructure file storage is implemented.
