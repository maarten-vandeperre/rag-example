## Summary
Added the `GetAdminProgress` use case so admin users can review overall document processing counts plus failed and in-flight document details.

## Changes
- `backend/src/main/java/com/rag/app/usecases/GetAdminProgress.java`
- `backend/src/main/java/com/rag/app/usecases/repositories/DocumentRepository.java`
- `backend/src/main/java/com/rag/app/usecases/models/GetAdminProgressInput.java`
- `backend/src/main/java/com/rag/app/usecases/models/GetAdminProgressOutput.java`
- `backend/src/main/java/com/rag/app/usecases/models/ProcessingStatistics.java`
- `backend/src/main/java/com/rag/app/usecases/models/FailedDocumentInfo.java`
- `backend/src/main/java/com/rag/app/usecases/models/ProcessingDocumentInfo.java`
- `backend/src/test/java/com/rag/app/usecases/GetAdminProgressTest.java`
- `backend/src/test/java/com/rag/app/usecases/GetUserDocumentsTest.java`
- `backend/src/test/java/com/rag/app/usecases/ProcessDocumentTest.java`
- `backend/src/test/java/com/rag/app/usecases/QueryDocumentsTest.java`
- `backend/src/test/java/com/rag/app/usecases/UploadDocumentTest.java`

## Impact
Admin-only progress monitoring is now available in the use case layer, and document repositories must provide aggregate progress statistics plus failed and processing document projections.

## Verification
- `mvn -s maven-settings.xml test`
- `mvn -s maven-settings.xml verify` (tests passed; Quarkus packaging step exceeded the CLI timeout after compilation and test execution)

## Follow-ups
- Add a concrete persistence adapter that maps failure timestamps and processing durations from storage once repository implementations are introduced.
