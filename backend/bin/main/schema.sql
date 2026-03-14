CREATE TABLE IF NOT EXISTS users (
    user_id VARCHAR(255) PRIMARY KEY,
    username VARCHAR(100) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    role VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true
);

CREATE TABLE IF NOT EXISTS documents (
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
    processing_started_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS chat_messages (
    message_id VARCHAR(255) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    question TEXT NOT NULL,
    answer TEXT,
    created_at TIMESTAMP NOT NULL,
    response_time_ms BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS document_references (
    reference_id VARCHAR(255) PRIMARY KEY,
    message_id VARCHAR(255) NOT NULL,
    reference_index INT NOT NULL DEFAULT 0,
    document_id VARCHAR(255) NOT NULL,
    document_name VARCHAR(500) NOT NULL,
    paragraph_reference TEXT,
    relevance_score DECIMAL(5,4),
    CONSTRAINT fk_document_references_message_id
        FOREIGN KEY (message_id) REFERENCES chat_messages(message_id)
);

CREATE INDEX IF NOT EXISTS idx_chat_messages_user_id_created_at
    ON chat_messages(user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_document_references_message_id
    ON document_references(message_id);
