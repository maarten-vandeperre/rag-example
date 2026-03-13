# Create Frontend API client

## Related User Story

User Story: upload_documents_and_chat_with_private_knowledge_base

## Objective

Create frontend API client to handle HTTP communication with the backend REST endpoints for document and chat functionality.

## Scope

- Create API client service for all backend endpoints
- Implement error handling and response mapping
- Add request/response interceptors for authentication
- Handle file upload with progress tracking

## Out of Scope

- Authentication token management
- Request caching mechanisms
- Offline functionality
- WebSocket connections

## Clean Architecture Placement

frontend API integration

## Execution Dependencies

- 0012-upload_documents_and_chat_with_private_knowledge_base-create_document_upload_rest_controller.md
- 0013-upload_documents_and_chat_with_private_knowledge_base-create_document_library_rest_controller.md
- 0014-upload_documents_and_chat_with_private_knowledge_base-create_chat_rest_controller.md

## Implementation Details

Create ApiClient service with methods:
- uploadDocument(file, onProgress)
- getUserDocuments(includeAll)
- getAdminProgress()
- submitChatQuery(question, maxResponseTimeMs)

Create DocumentApiClient with:
- POST /api/documents/upload
- GET /api/documents
- GET /api/admin/documents/progress
- File upload with progress tracking
- Multipart form data handling

Create ChatApiClient with:
- POST /api/chat/query
- Request timeout handling (20 seconds)
- Response streaming support (if needed)

Error handling:
- HTTP status code mapping to user-friendly messages
- Network error handling
- Timeout error handling
- Validation error display

Request/Response interceptors:
- Add authentication headers
- Handle common error responses
- Log requests for debugging
- Transform response data

File upload handling:
- Multipart form data creation
- Upload progress tracking
- File validation before upload
- Cancel upload functionality

## Files / Modules Impacted

- frontend/src/services/ApiClient.js
- frontend/src/services/DocumentApiClient.js
- frontend/src/services/ChatApiClient.js
- frontend/src/services/ErrorHandler.js
- frontend/src/utils/HttpClient.js

## Acceptance Criteria

Given a document upload is initiated
When uploadDocument() is called with a file
Then the file should be uploaded with progress tracking

Given user documents are requested
When getUserDocuments() is called
Then the user's documents should be returned

Given a chat query is submitted
When submitChatQuery() is called
Then the query should be sent and response returned

Given a network error occurs
When any API call fails
Then appropriate error message should be returned

## Testing Requirements

- Unit tests for ApiClient service
- Unit tests for error handling
- Mock tests for HTTP requests
- Integration tests with mock backend
- Tests for file upload progress

## Dependencies / Preconditions

- React application must be set up
- HTTP client library (axios or fetch) must be available
- Backend REST endpoints must be available