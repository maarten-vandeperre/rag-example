## Summary
Added the upload document use case with request and response models, duplicate detection, user validation, and repository contracts for persistence.

## Changes
- `backend/src/main/java/com/rag/app/usecases/UploadDocument.java`
- `backend/src/main/java/com/rag/app/usecases/models/UploadDocumentInput.java`
- `backend/src/main/java/com/rag/app/usecases/models/UploadDocumentOutput.java`
- `backend/src/main/java/com/rag/app/usecases/repositories/DocumentRepository.java`
- `backend/src/main/java/com/rag/app/usecases/repositories/UserRepository.java`
- `backend/src/test/java/com/rag/app/usecases/UploadDocumentTest.java`

## Impact
The backend application layer can now validate uploads, enforce the 40MB limit, reject duplicate content, and create initial document records for active users.

## Verification
- `mvn -s maven-settings.xml -U compile`
- `mvn -s maven-settings.xml -U test`

## Follow-ups
- Add concrete repository implementations and file storage integration in the infrastructure layer.
- Consider replacing exception-based validation with a richer result model if API error mapping becomes more complex.
