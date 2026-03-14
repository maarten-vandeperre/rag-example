# Create Chat REST controller

## Related User Story

User Story: upload_documents_and_chat_with_private_knowledge_base

## Objective

Create REST controller for chat functionality to handle user questions and return answers with document references.

## Scope

- Create REST controller for POST /api/chat/query
- Implement request/response handling for chat queries
- Add timeout handling for 20-second response requirement
- Map between REST DTOs and use case models

## Out of Scope

- Real-time chat features
- Chat session management
- Message history endpoints
- WebSocket implementation

## Clean Architecture Placement

interface adapters

## Execution Dependencies

- 0006-upload_documents_and_chat_with_private_knowledge_base-create_query_documents_usecase.md

## Implementation Details

Create ChatController with:
- POST /api/chat/query endpoint
- Request timeout handling (20 seconds)
- User context extraction from authentication
- Error handling for query failures

Create ChatQueryRequest DTO with:
- question (string, required)
- maxResponseTimeMs (int, optional, default 20000)

Create ChatQueryResponse DTO with:
- answer (string)
- documentReferences (list of DocumentReferenceDto)
- responseTimeMs (int)
- success (boolean)

Create DocumentReferenceDto with:
- documentId (string)
- documentName (string)
- paragraphReference (string)
- relevanceScore (double)

HTTP status codes:
- 200 OK: successful query with answer
- 400 Bad Request: invalid question or request format
- 408 Request Timeout: query exceeded 20-second limit
- 404 Not Found: no relevant documents found
- 500 Internal Server Error: processing error

Error handling:
- Empty or null question: return 400 with validation error
- No relevant documents: return 404 with "no answer found" message
- Query timeout: return 408 with timeout message
- Processing error: return 500 with generic error message

## Files / Modules Impacted

- backend/adapters/rest/ChatController.java
- backend/adapters/rest/dto/ChatQueryRequest.java
- backend/adapters/rest/dto/ChatQueryResponse.java
- backend/adapters/rest/dto/DocumentReferenceDto.java

## Acceptance Criteria

Given a valid chat query is submitted
When POST /api/chat/query is called
Then an answer with source references should be returned

Given a query with no relevant documents
When POST /api/chat/query is called
Then HTTP 404 should be returned with "no answer found" message

Given a query takes longer than 20 seconds
When POST /api/chat/query is called
Then HTTP 408 should be returned with timeout message

Given an empty question is submitted
When POST /api/chat/query is called
Then HTTP 400 should be returned with validation error

## Testing Requirements

- Unit tests for ChatController
- Integration tests for chat query endpoint
- Tests for timeout handling
- Tests for error response formats
- Tests for document reference mapping

## Dependencies / Preconditions

- QueryDocuments use case must be implemented
- User authentication must be available
- Request timeout configuration must be set