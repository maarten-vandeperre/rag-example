# Implement ChatMessage repository with JDBC

## Related User Story

User Story: upload_documents_and_chat_with_private_knowledge_base

## Objective

Implement the ChatMessageRepository interface using JDBC for chat message persistence and retrieval.

## Scope

- Implement ChatMessageRepository interface using JDBC
- Create database schema for chat_messages and document_references tables
- Implement chat message CRUD operations with explicit SQL
- Handle document references as related entities

## Out of Scope

- Chat session management
- Real-time message updates
- Message search functionality
- Message deletion policies

## Clean Architecture Placement

infrastructure

## Execution Dependencies

- 0003-upload_documents_and_chat_with_private_knowledge_base-create_chat_message_domain_entity.md

## Implementation Details

Create chat_messages table schema:
```sql
CREATE TABLE chat_messages (
    message_id VARCHAR(255) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    question TEXT NOT NULL,
    answer TEXT,
    created_at TIMESTAMP NOT NULL,
    response_time_ms INTEGER
);

CREATE TABLE document_references (
    reference_id VARCHAR(255) PRIMARY KEY,
    message_id VARCHAR(255) NOT NULL,
    document_id VARCHAR(255) NOT NULL,
    document_name VARCHAR(500) NOT NULL,
    paragraph_reference TEXT,
    relevance_score DECIMAL(5,4),
    FOREIGN KEY (message_id) REFERENCES chat_messages(message_id)
);
```

Create ChatMessageRepository interface with methods:
- save(ChatMessage message)
- findById(String messageId)
- findByUserId(String userId)
- findRecentByUserId(String userId, int limit)

Implement JdbcChatMessageRepository with:
- Explicit SQL statements for all operations
- Proper handling of document references as child entities
- Transaction management for message and references
- Proper SQLException handling

## Files / Modules Impacted

- backend/usecases/repositories/ChatMessageRepository.java
- backend/infrastructure/persistence/JdbcChatMessageRepository.java
- backend/infrastructure/persistence/ChatMessageRowMapper.java
- backend/infrastructure/persistence/DocumentReferenceRowMapper.java
- backend/infrastructure/database/schema.sql (extend existing)

## Acceptance Criteria

Given a ChatMessage with document references is saved
When save() method is called
Then both message and references should be persisted

Given a chat message exists
When findById() is called with valid ID
Then the message with all references should be returned

Given multiple messages exist for a user
When findByUserId() is called
Then all user's messages should be returned with references

Given recent messages are requested
When findRecentByUserId() is called with limit
Then the most recent messages should be returned

## Testing Requirements

- Unit tests for JdbcChatMessageRepository
- Integration tests with in-memory database
- Tests for transaction handling
- Tests for document reference persistence
- Tests for error handling

## Dependencies / Preconditions

- ChatMessage and DocumentReference entities must exist
- Database connection configuration must be available