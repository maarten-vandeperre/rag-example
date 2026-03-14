# Create QueryDocuments use case

## Related User Story

User Story: upload_documents_and_chat_with_private_knowledge_base

## Objective

Create the QueryDocuments use case that handles chat questions by searching through user's documents and returning answers with source references.

## Scope

- Create QueryDocuments use case with input/output models
- Implement role-based document access (standard users see only their documents, admins see all)
- Define interface for semantic search and answer generation
- Include source document references in responses

## Out of Scope

- Actual semantic search implementation
- LLM integration for answer generation
- Real-time chat features
- Response time optimization

## Clean Architecture Placement

usecases

## Execution Dependencies

- 0007-upload_documents_and_chat_with_private_knowledge_base-create_document_domain_entity.md
- 0008-upload_documents_and_chat_with_private_knowledge_base-create_user_domain_entity.md
- 0009-upload_documents_and_chat_with_private_knowledge_base-create_chat_message_domain_entity.md

## Implementation Details

Create QueryDocumentsInput with:
- userId (string)
- question (string)
- maxResponseTimeMs (int, default 20000)

Create QueryDocumentsOutput with:
- answer (string)
- documentReferences (list of DocumentReference)
- responseTimeMs (int)
- success (boolean)
- errorMessage (string, if failed)

Create SemanticSearch interface with:
- searchDocuments(String query, List<String> documentIds) method
- returns list of relevant document chunks with scores

Create AnswerGenerator interface with:
- generateAnswer(String question, List<DocumentChunk> context) method
- returns answer with source references

Business rules:
- Standard users can only query their own READY documents
- Admin users can query all READY documents
- Response must include source document name and paragraph reference
- Response time should not exceed 20 seconds
- If no relevant documents found, return appropriate message

## Files / Modules Impacted

- backend/usecases/QueryDocuments.java
- backend/usecases/models/QueryDocumentsInput.java
- backend/usecases/models/QueryDocumentsOutput.java
- backend/usecases/interfaces/SemanticSearch.java
- backend/usecases/interfaces/AnswerGenerator.java
- backend/usecases/models/DocumentChunk.java

## Acceptance Criteria

Given a standard user queries their documents
When the user has READY documents
Then the system should search only the user's documents

Given an admin user queries documents
When the admin submits a question
Then the system should search all READY documents

Given a successful query
When relevant documents are found
Then the response should include source references

Given a query with no relevant documents
When no documents match the question
Then the system should return an appropriate "no answer found" message

## Testing Requirements

- Unit tests for QueryDocuments use case
- Unit tests for role-based access control
- Unit tests for response time tracking
- Mock tests for SemanticSearch interface
- Mock tests for AnswerGenerator interface

## Dependencies / Preconditions

- Document, User, and ChatMessage entities must exist
- DocumentRepository interface must exist