# Create ChatMessage domain entity

## Related User Story

User Story: upload_documents_and_chat_with_private_knowledge_base

## Objective

Create the ChatMessage domain entity to represent chat interactions between users and the system, including source document references.

## Scope

- Create ChatMessage domain entity with essential properties
- Include DocumentReference value object for source citations
- Add basic validation rules

## Out of Scope

- Chat session management
- Message persistence
- Real-time chat features
- Frontend chat components

## Clean Architecture Placement

domain

## Execution Dependencies

- 0007-upload_documents_and_chat_with_private_knowledge_base-create_document_domain_entity.md

## Implementation Details

Create a ChatMessage entity with the following properties:
- messageId (unique identifier)
- userId (who sent the message)
- question (user's question text)
- answer (system's response)
- documentReferences (list of source document references)
- createdAt (timestamp)
- responseTimeMs (response time in milliseconds)

Create DocumentReference value object with:
- documentId (reference to source document)
- documentName (name of referenced document)
- paragraphReference (specific section or paragraph)
- relevanceScore (how relevant this source is)

Validation rules:
- messageId must not be null
- userId must not be null
- question must not be null or empty
- createdAt must not be null
- responseTimeMs must be positive
- documentReferences can be empty but not null

## Files / Modules Impacted

- backend/domain/entities/ChatMessage.java
- backend/domain/valueobjects/DocumentReference.java

## Acceptance Criteria

Given a ChatMessage entity is created
When all required properties are provided with valid values
Then the entity should be successfully instantiated

Given a ChatMessage entity is created with null question
When question is null or empty
Then validation should fail

Given a DocumentReference is created
When documentId and documentName are provided
Then the reference should be valid

## Testing Requirements

- Unit tests for ChatMessage entity creation
- Unit tests for validation rules
- Unit tests for DocumentReference value object

## Dependencies / Preconditions

- Document entity must exist
- Database schema must be defined