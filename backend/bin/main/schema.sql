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
    processing_started_at TIMESTAMP,
    knowledge_processing_status VARCHAR(50) NOT NULL DEFAULT 'NOT_STARTED',
    knowledge_processing_warnings TEXT,
    knowledge_processing_error TEXT,
    knowledge_processing_started_at TIMESTAMP,
    knowledge_processing_completed_at TIMESTAMP,
    associated_graph_id VARCHAR(255)
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

CREATE TABLE IF NOT EXISTS answer_source_references (
    reference_id VARCHAR(255) PRIMARY KEY,
    answer_id VARCHAR(255) NOT NULL,
    document_id VARCHAR(255) NOT NULL,
    chunk_id VARCHAR(255) NOT NULL,
    snippet_content TEXT NOT NULL,
    snippet_context TEXT,
    start_position INT,
    end_position INT,
    relevance_score DECIMAL(5,4) NOT NULL,
    source_order INT NOT NULL,
    document_title VARCHAR(500),
    document_filename VARCHAR(500),
    document_file_type VARCHAR(50),
    page_number INT,
    chunk_index INT,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_answer_source_references_answer_id
        FOREIGN KEY (answer_id) REFERENCES chat_messages(message_id)
);

CREATE INDEX IF NOT EXISTS idx_answer_source_references_answer_id
    ON answer_source_references(answer_id, source_order);

-- Answer Source References Table for detailed chunk-level source tracking
CREATE TABLE IF NOT EXISTS answer_source_references (
    id VARCHAR(255) PRIMARY KEY,
    answer_id VARCHAR(255) NOT NULL,
    document_id VARCHAR(255),
    chunk_id VARCHAR(255) NOT NULL,
    snippet_content TEXT NOT NULL,
    snippet_context TEXT,
    start_position INTEGER,
    end_position INTEGER,
    relevance_score DECIMAL(5,4) NOT NULL,
    source_order INTEGER NOT NULL,
    document_title VARCHAR(500),
    document_filename VARCHAR(255),
    document_file_type VARCHAR(50),
    page_number INTEGER,
    chunk_index INTEGER,
    created_at TIMESTAMP NOT NULL,
    
    CONSTRAINT fk_answer_source_answer 
        FOREIGN KEY (answer_id) REFERENCES chat_messages(message_id) ON DELETE CASCADE,
    CONSTRAINT fk_answer_source_document 
        FOREIGN KEY (document_id) REFERENCES documents(document_id) ON DELETE SET NULL,
    CONSTRAINT chk_relevance_score 
        CHECK (relevance_score >= 0.0 AND relevance_score <= 1.0),
    CONSTRAINT chk_source_order 
        CHECK (source_order >= 0),
    CONSTRAINT chk_positions 
        CHECK (start_position IS NULL OR end_position IS NULL OR start_position <= end_position)
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_answer_source_references_answer_id ON answer_source_references(answer_id);
CREATE INDEX IF NOT EXISTS idx_answer_source_references_document_id ON answer_source_references(document_id);
CREATE INDEX IF NOT EXISTS idx_answer_source_references_chunk_id ON answer_source_references(chunk_id);
CREATE INDEX IF NOT EXISTS idx_answer_source_references_source_order ON answer_source_references(answer_id, source_order);
CREATE INDEX IF NOT EXISTS idx_answer_source_references_created_at ON answer_source_references(created_at);
