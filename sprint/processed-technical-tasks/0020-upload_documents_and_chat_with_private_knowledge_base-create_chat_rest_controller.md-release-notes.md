## Summary
Added a chat query REST controller with DTO mapping, authentication-based user extraction, timeout handling, and HTTP responses for success and common failure cases.

## Changes
- `backend/src/main/java/com/rag/app/api/ChatController.java`
- `backend/src/main/java/com/rag/app/api/dto/ChatQueryRequest.java`
- `backend/src/main/java/com/rag/app/api/dto/ChatQueryResponse.java`
- `backend/src/main/java/com/rag/app/api/dto/DocumentReferenceDto.java`
- `backend/src/test/java/com/rag/app/api/ChatControllerTest.java`
- `backend/src/main/java/com/rag/app/usecases/UploadDocument.java`
- `backend/src/main/resources/application.properties`

## Impact
The backend now exposes `POST /api/chat/query`, translates use case results into REST responses, and enforces request-level timeout behavior while preserving source reference data in the API response.

## Verification
- `mvn -s maven-settings.xml -Dtest=DocumentUploadControllerTest test`
- `mvn -s maven-settings.xml test`
- `mvn -s maven-settings.xml compile && mvn -s maven-settings.xml test`

## Follow-ups
- Wire authenticated principals into the runtime security layer so the controller can be exercised end-to-end through HTTP integration tests.
