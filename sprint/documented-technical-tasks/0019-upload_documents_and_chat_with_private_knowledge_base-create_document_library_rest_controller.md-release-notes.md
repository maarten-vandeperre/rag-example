## Summary
Added a document library REST resource with DTO mapping for user document listings and admin progress retrieval, including role-aware response handling for the new document library endpoints.

## Changes
Created `backend/src/main/java/com/rag/app/api/DocumentLibraryResource.java`, `backend/src/main/java/com/rag/app/api/dto/DocumentListResponse.java`, `backend/src/main/java/com/rag/app/api/dto/DocumentSummaryDto.java`, `backend/src/main/java/com/rag/app/api/dto/AdminProgressResponse.java`, `backend/src/main/java/com/rag/app/api/dto/ProcessingStatisticsDto.java`, `backend/src/main/java/com/rag/app/api/dto/FailedDocumentDto.java`, `backend/src/main/java/com/rag/app/api/dto/ProcessingDocumentDto.java`, and `backend/src/test/java/com/rag/app/api/DocumentLibraryResourceTest.java`.

## Impact
The backend now exposes `/api/documents` and `/api/admin/documents/progress` adapter endpoints that translate use case output into REST-friendly payloads and return a forbidden response for non-admin access to progress data.

## Verification
Executed `mvn -gs maven-settings.xml -s maven-settings.xml -DskipTests compile` and `mvn -gs maven-settings.xml -s maven-settings.xml -Dtest=BackendStatusResourceTest,DocumentLibraryResourceTest,GetUserDocumentsTest,GetAdminProgressTest test` in `backend/`.

## Follow-ups
Connect the temporary `X-User-Id` header-based access pattern to the eventual authentication/security integration when user identity is available through the runtime security context.
