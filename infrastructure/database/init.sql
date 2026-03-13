CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE users (
    user_id VARCHAR(255) PRIMARY KEY,
    username VARCHAR(100) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    role VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true
);

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

CREATE TABLE chat_messages (
    message_id VARCHAR(255) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    question TEXT NOT NULL,
    answer TEXT,
    created_at TIMESTAMP NOT NULL,
    response_time_ms INTEGER,
    FOREIGN KEY (user_id) REFERENCES users(user_id)
);

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

CREATE TABLE document_chunks (
    chunk_id VARCHAR(255) PRIMARY KEY,
    document_id VARCHAR(255) NOT NULL,
    chunk_index INTEGER NOT NULL,
    text_content TEXT NOT NULL,
    embedding vector(384),
    created_at TIMESTAMP NOT NULL,
    FOREIGN KEY (document_id) REFERENCES documents(document_id)
);

CREATE INDEX idx_documents_uploaded_by ON documents(uploaded_by);
CREATE INDEX idx_documents_status ON documents(status);
CREATE INDEX idx_chat_messages_user_id ON chat_messages(user_id);
CREATE INDEX idx_document_chunks_document_id ON document_chunks(document_id);
CREATE INDEX idx_document_chunks_embedding ON document_chunks USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);
