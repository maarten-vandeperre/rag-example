## Summary
Added the `GetUserDocuments` use case and supporting models so document library queries can enforce standard-user scoping while allowing admins to optionally retrieve every document.

## Changes
Created `backend/src/main/java/com/rag/app/usecases/GetUserDocuments.java`, `backend/src/main/java/com/rag/app/usecases/models/GetUserDocumentsInput.java`, `backend/src/main/java/com/rag/app/usecases/models/GetUserDocumentsOutput.java`, `backend/src/main/java/com/rag/app/usecases/models/DocumentSummary.java`, and `backend/src/test/java/com/rag/app/usecases/GetUserDocumentsTest.java`.
Extended `backend/src/main/java/com/rag/app/usecases/repositories/DocumentRepository.java` and updated in-memory repository test doubles in `backend/src/test/java/com/rag/app/usecases/UploadDocumentTest.java` and `backend/src/test/java/com/rag/app/usecases/ProcessDocumentTest.java`.

## Impact
Backend use cases can now return role-filtered document summaries sorted by newest upload first, with status and metadata ready for a document library response model.

## Verification
Executed `mvn -gs maven-settings.xml -s maven-settings.xml test` in `backend/`.

## Follow-ups
Wire the use case into an application service or API resource once the document library endpoint is implemented.
