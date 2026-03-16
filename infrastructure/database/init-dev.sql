-- Development database initialization for local Podman services.

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS users (
    user_id VARCHAR(255) PRIMARY KEY,
    username VARCHAR(100) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    role VARCHAR(50) NOT NULL DEFAULT 'STANDARD',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT true,
    keycloak_user_id VARCHAR(255) UNIQUE,
    CONSTRAINT users_role_check CHECK (role IN ('STANDARD', 'ADMIN'))
);

CREATE TABLE IF NOT EXISTS documents (
    document_id VARCHAR(255) PRIMARY KEY,
    file_name VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL,
    file_type VARCHAR(50) NOT NULL,
    uploaded_by VARCHAR(255) NOT NULL,
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(50) NOT NULL DEFAULT 'UPLOADED',
    content_hash VARCHAR(255),
    last_updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    failure_reason TEXT,
    processing_started_at TIMESTAMP,
    processing_completed_at TIMESTAMP,
    CONSTRAINT documents_file_size_check CHECK (file_size > 0 AND file_size <= 41943040),
    CONSTRAINT documents_file_type_check CHECK (file_type IN ('PDF', 'MARKDOWN', 'PLAIN_TEXT')),
    CONSTRAINT documents_status_check CHECK (status IN ('UPLOADED', 'PROCESSING', 'READY', 'FAILED')),
    FOREIGN KEY (uploaded_by) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS chat_messages (
    message_id VARCHAR(255) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    question TEXT NOT NULL,
    answer TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    response_time_ms INTEGER,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS document_references (
    reference_id VARCHAR(255) PRIMARY KEY,
    message_id VARCHAR(255) NOT NULL,
    reference_index INTEGER NOT NULL DEFAULT 0,
    document_id VARCHAR(255) NOT NULL,
    document_name VARCHAR(500) NOT NULL,
    paragraph_reference TEXT,
    relevance_score DECIMAL(5,4),
    FOREIGN KEY (message_id) REFERENCES chat_messages(message_id) ON DELETE CASCADE,
    FOREIGN KEY (document_id) REFERENCES documents(document_id) ON DELETE CASCADE
);

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
    FOREIGN KEY (answer_id) REFERENCES chat_messages(message_id) ON DELETE CASCADE,
    FOREIGN KEY (document_id) REFERENCES documents(document_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS document_chunks (
    chunk_id VARCHAR(255) PRIMARY KEY,
    document_id VARCHAR(255) NOT NULL,
    chunk_index INTEGER NOT NULL,
    text_content TEXT NOT NULL,
    embedding vector(384),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (document_id) REFERENCES documents(document_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS user_sessions (
    session_id VARCHAR(255) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_documents_uploaded_by ON documents(uploaded_by);
CREATE INDEX IF NOT EXISTS idx_documents_status ON documents(status);
CREATE INDEX IF NOT EXISTS idx_documents_uploaded_at ON documents(uploaded_at DESC);
CREATE INDEX IF NOT EXISTS idx_chat_messages_user_id ON chat_messages(user_id);
CREATE INDEX IF NOT EXISTS idx_chat_messages_created_at ON chat_messages(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_document_references_message_id ON document_references(message_id, reference_index);
CREATE INDEX IF NOT EXISTS idx_document_chunks_document_id ON document_chunks(document_id);
CREATE INDEX IF NOT EXISTS idx_document_chunks_embedding ON document_chunks USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);
CREATE INDEX IF NOT EXISTS idx_answer_source_references_answer_id ON answer_source_references(answer_id, source_order);
CREATE INDEX IF NOT EXISTS idx_user_sessions_user_id ON user_sessions(user_id);
CREATE INDEX IF NOT EXISTS idx_user_sessions_expires_at ON user_sessions(expires_at);

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS update_users_updated_at ON users;
CREATE TRIGGER update_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_documents_updated_at ON documents;
CREATE TRIGGER update_documents_updated_at
    BEFORE UPDATE ON documents
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE OR REPLACE VIEW user_document_summary AS
SELECT
    u.username,
    u.email,
    u.role,
    COUNT(d.document_id) AS total_documents,
    COUNT(CASE WHEN d.status = 'READY' THEN 1 END) AS ready_documents,
    COUNT(CASE WHEN d.status = 'FAILED' THEN 1 END) AS failed_documents,
    MAX(d.uploaded_at) AS last_upload
FROM users u
LEFT JOIN documents d ON u.user_id = d.uploaded_by
GROUP BY u.user_id, u.username, u.email, u.role;

CREATE OR REPLACE VIEW document_processing_status AS
SELECT
    status,
    COUNT(*) AS count,
    AVG(file_size) AS avg_file_size,
    MIN(uploaded_at) AS oldest_upload,
    MAX(uploaded_at) AS newest_upload
FROM documents
GROUP BY status;

CREATE OR REPLACE FUNCTION reset_dev_data()
RETURNS void AS $$
BEGIN
    TRUNCATE TABLE answer_source_references CASCADE;
    TRUNCATE TABLE document_references CASCADE;
    TRUNCATE TABLE chat_messages CASCADE;
    TRUNCATE TABLE document_chunks CASCADE;
    TRUNCATE TABLE documents CASCADE;
    TRUNCATE TABLE user_sessions CASCADE;
    TRUNCATE TABLE users CASCADE;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION generate_test_data()
RETURNS void AS $$
BEGIN
    RAISE NOTICE 'Use sample-dev-data.sql to generate test data';
END;
$$ LANGUAGE plpgsql;
