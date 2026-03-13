## Summary
Added the document upload REST controller with multipart request handling, file validation, DTO mapping, and structured error responses.

## Changes
- `backend/src/main/java/com/rag/app/api/DocumentUploadController.java`
- `backend/src/main/java/com/rag/app/api/dto/UploadDocumentRequest.java`
- `backend/src/main/java/com/rag/app/api/dto/UploadDocumentResponse.java`
- `backend/src/main/java/com/rag/app/api/dto/ErrorResponse.java`
- `backend/src/main/java/com/rag/app/usecases/UploadDocument.java`
- `backend/src/test/java/com/rag/app/api/DocumentUploadControllerTest.java`

## Impact
The backend now exposes `POST /api/documents/upload`, validates uploaded files before invoking the use case, and returns consistent success and error payloads for clients.

## Verification
- `mvn -s maven-settings.xml -U compile`
- `mvn -s maven-settings.xml -U test`

## Follow-ups
- Replace the temporary userId form field with authentication-context integration when security is added.
- Add end-to-end multipart endpoint tests once the full runtime upload pipeline is wired into infrastructure storage.
