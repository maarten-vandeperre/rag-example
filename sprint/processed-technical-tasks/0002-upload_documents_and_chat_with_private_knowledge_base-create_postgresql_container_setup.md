# Create PostgreSQL container setup

## Related User Story

User Story: upload_documents_and_chat_with_private_knowledge_base

## Objective

Create PostgreSQL container configuration with pgvector extension for document metadata storage and vector search capabilities.

## Scope

- Create PostgreSQL Dockerfile with pgvector extension
- Create database initialization scripts
- Configure database schema for application tables
- Set up database connection configuration

## Out of Scope

- Database backup and recovery
- Database performance tuning
- Database monitoring setup
- Production database clustering

## Clean Architecture Placement

infrastructure

## Execution Dependencies

- 0001-upload_documents_and_chat_with_private_knowledge_base-create_podman_compose_configuration.md

## Implementation Details

Create PostgreSQL setup with:

1. **Custom Dockerfile** (if needed for pgvector):
```dockerfile
FROM postgres:15-alpine
RUN apk add --no-cache git build-base postgresql-dev
RUN git clone https://github.com/pgvector/pgvector.git
RUN cd pgvector && make && make install
```

2. **Database initialization script** (init.sql):
```sql
-- Create pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;

-- Create application database
CREATE DATABASE rag_app;

-- Connect to application database
\c rag_app;

-- Create users table
CREATE TABLE users (
    user_id VARCHAR(255) PRIMARY KEY,
    username VARCHAR(100) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    role VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true
);

-- Create documents table
CREATE TABLE documents (
    document_id VARCHAR(255) PRIMARY KEY,
    file_name VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL,
    file_type VARCHAR(50) NOT NULL,
    uploaded_by VARCHAR(255) NOT NULL,
    uploaded_at TIMESTAMP NOT NULL,
    status VARCHAR(50) NOT NULL,
    content_hash VARCHAR(255),
    last_updated TIMESTAMP NOT NULL,
    failure_reason TEXT,
    processing_started_at TIMESTAMP,
    FOREIGN KEY (uploaded_by) REFERENCES users(user_id)
);

-- Create chat_messages table
CREATE TABLE chat_messages (
    message_id VARCHAR(255) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    question TEXT NOT NULL,
    answer TEXT,
    created_at TIMESTAMP NOT NULL,
    response_time_ms INTEGER,
    FOREIGN KEY (user_id) REFERENCES users(user_id)
);

-- Create document_references table
CREATE TABLE document_references (
    reference_id VARCHAR(255) PRIMARY KEY,
    message_id VARCHAR(255) NOT NULL,
    document_id VARCHAR(255) NOT NULL,
    document_name VARCHAR(500) NOT NULL,
    paragraph_reference TEXT,
    relevance_score DECIMAL(5,4),
    FOREIGN KEY (message_id) REFERENCES chat_messages(message_id),
    FOREIGN KEY (document_id) REFERENCES documents(document_id)
);

-- Create document_chunks table for vector storage
CREATE TABLE document_chunks (
    chunk_id VARCHAR(255) PRIMARY KEY,
    document_id VARCHAR(255) NOT NULL,
    chunk_index INTEGER NOT NULL,
    text_content TEXT NOT NULL,
    embedding vector(384), -- Adjust dimension based on embedding model
    created_at TIMESTAMP NOT NULL,
    FOREIGN KEY (document_id) REFERENCES documents(document_id)
);

-- Create indexes for performance
CREATE INDEX idx_documents_uploaded_by ON documents(uploaded_by);
CREATE INDEX idx_documents_status ON documents(status);
CREATE INDEX idx_chat_messages_user_id ON chat_messages(user_id);
CREATE INDEX idx_document_chunks_document_id ON document_chunks(document_id);
CREATE INDEX idx_document_chunks_embedding ON document_chunks USING ivfflat (embedding vector_cosine_ops);
```

3. **Sample data script** (sample-data.sql):
```sql
-- Insert sample users
INSERT INTO users (user_id, username, email, role, created_at, is_active) VALUES
('user-1', 'john_doe', 'john@example.com', 'STANDARD', NOW(), true),
('user-2', 'jane_admin', 'jane@example.com', 'ADMIN', NOW(), true);
```

Environment configuration:
- POSTGRES_DB=rag_app
- POSTGRES_USER=rag_user
- POSTGRES_PASSWORD=rag_password
- POSTGRES_INITDB_ARGS=--encoding=UTF-8

## Files / Modules Impacted

- infrastructure/database/Dockerfile (if custom image needed)
- infrastructure/database/init.sql
- infrastructure/database/sample-data.sql
- docker-compose.yml (postgres service configuration)

## Acceptance Criteria

Given PostgreSQL container is started
When initialization scripts run
Then all tables should be created successfully

Given pgvector extension is installed
When vector operations are performed
Then vector similarity search should work

Given sample data is loaded
When application connects to database
Then sample users should be available

Given database container is restarted
When data persistence is tested
Then all data should be preserved

## Testing Requirements

- Test database container startup
- Test schema creation and initialization
- Test pgvector extension functionality
- Test data persistence across restarts
- Test database connection from application

## Dependencies / Preconditions

- Podman Compose configuration must exist
- PostgreSQL image must be available
- pgvector extension must be compatible with PostgreSQL version