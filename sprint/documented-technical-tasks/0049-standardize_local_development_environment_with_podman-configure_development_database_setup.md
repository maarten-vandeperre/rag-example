# Configure Development Database Setup

## Related User Story

User Story: standardize_local_development_environment_with_podman

## Objective

Configure PostgreSQL database setup specifically for local development with proper schema initialization, sample data, and development-specific configurations.

## Scope

- Create development database initialization scripts
- Configure database schema for development environment
- Add sample development data for testing
- Set up database migration and seeding scripts
- Configure development-specific database settings

## Out of Scope

- Production database configuration
- Database backup and recovery procedures
- Performance tuning for production
- Database monitoring and alerting

## Clean Architecture Placement

infrastructure

## Execution Dependencies

- 0047-standardize_local_development_environment_with_podman-create_development_services_compose.md

## Implementation Details

Create development database initialization script (infrastructure/database/init-dev.sql):
```sql
-- Development Database Initialization Script
-- This script sets up the database schema for local development

-- Create extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS vector;

-- Create application database if not exists
-- (This is handled by docker-compose environment variables)

-- Connect to the application database
\c rag_app_dev;

-- Create users table
CREATE TABLE IF NOT EXISTS users (
    user_id VARCHAR(255) PRIMARY KEY DEFAULT uuid_generate_v4()::text,
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

-- Create documents table
CREATE TABLE IF NOT EXISTS documents (
    document_id VARCHAR(255) PRIMARY KEY DEFAULT uuid_generate_v4()::text,
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

-- Create chat_messages table
CREATE TABLE IF NOT EXISTS chat_messages (
    message_id VARCHAR(255) PRIMARY KEY DEFAULT uuid_generate_v4()::text,
    user_id VARCHAR(255) NOT NULL,
    question TEXT NOT NULL,
    answer TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    response_time_ms INTEGER,
    
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- Create document_references table
CREATE TABLE IF NOT EXISTS document_references (
    reference_id VARCHAR(255) PRIMARY KEY DEFAULT uuid_generate_v4()::text,
    message_id VARCHAR(255) NOT NULL,
    document_id VARCHAR(255) NOT NULL,
    document_name VARCHAR(500) NOT NULL,
    paragraph_reference TEXT,
    relevance_score DECIMAL(5,4),
    
    FOREIGN KEY (message_id) REFERENCES chat_messages(message_id) ON DELETE CASCADE,
    FOREIGN KEY (document_id) REFERENCES documents(document_id) ON DELETE CASCADE
);

-- Create document_chunks table for vector storage
CREATE TABLE IF NOT EXISTS document_chunks (
    chunk_id VARCHAR(255) PRIMARY KEY DEFAULT uuid_generate_v4()::text,
    document_id VARCHAR(255) NOT NULL,
    chunk_index INTEGER NOT NULL,
    text_content TEXT NOT NULL,
    embedding vector(384), -- Adjust dimension based on embedding model
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (document_id) REFERENCES documents(document_id) ON DELETE CASCADE
);

-- Create user_sessions table for session management
CREATE TABLE IF NOT EXISTS user_sessions (
    session_id VARCHAR(255) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_documents_uploaded_by ON documents(uploaded_by);
CREATE INDEX IF NOT EXISTS idx_documents_status ON documents(status);
CREATE INDEX IF NOT EXISTS idx_documents_uploaded_at ON documents(uploaded_at DESC);
CREATE INDEX IF NOT EXISTS idx_chat_messages_user_id ON chat_messages(user_id);
CREATE INDEX IF NOT EXISTS idx_chat_messages_created_at ON chat_messages(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_document_chunks_document_id ON document_chunks(document_id);
CREATE INDEX IF NOT EXISTS idx_document_chunks_embedding ON document_chunks USING ivfflat (embedding vector_cosine_ops);
CREATE INDEX IF NOT EXISTS idx_user_sessions_user_id ON user_sessions(user_id);
CREATE INDEX IF NOT EXISTS idx_user_sessions_expires_at ON user_sessions(expires_at);

-- Create updated_at trigger function
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create triggers for updated_at
CREATE TRIGGER update_users_updated_at 
    BEFORE UPDATE ON users 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_documents_updated_at 
    BEFORE UPDATE ON documents 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Grant permissions
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO rag_dev_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO rag_dev_user;
GRANT ALL PRIVILEGES ON ALL FUNCTIONS IN SCHEMA public TO rag_dev_user;

-- Create development views for easier querying
CREATE OR REPLACE VIEW user_document_summary AS
SELECT 
    u.username,
    u.email,
    u.role,
    COUNT(d.document_id) as total_documents,
    COUNT(CASE WHEN d.status = 'READY' THEN 1 END) as ready_documents,
    COUNT(CASE WHEN d.status = 'FAILED' THEN 1 END) as failed_documents,
    MAX(d.uploaded_at) as last_upload
FROM users u
LEFT JOIN documents d ON u.user_id = d.uploaded_by
GROUP BY u.user_id, u.username, u.email, u.role;

CREATE OR REPLACE VIEW document_processing_status AS
SELECT 
    status,
    COUNT(*) as count,
    AVG(file_size) as avg_file_size,
    MIN(uploaded_at) as oldest_upload,
    MAX(uploaded_at) as newest_upload
FROM documents
GROUP BY status;

-- Development utility functions
CREATE OR REPLACE FUNCTION reset_dev_data()
RETURNS void AS $$
BEGIN
    -- Clear all data (be careful!)
    TRUNCATE TABLE document_references CASCADE;
    TRUNCATE TABLE chat_messages CASCADE;
    TRUNCATE TABLE document_chunks CASCADE;
    TRUNCATE TABLE documents CASCADE;
    TRUNCATE TABLE user_sessions CASCADE;
    TRUNCATE TABLE users CASCADE;
    
    RAISE NOTICE 'All development data has been cleared';
END;
$$ LANGUAGE plpgsql;

-- Function to generate test data
CREATE OR REPLACE FUNCTION generate_test_data()
RETURNS void AS $$
BEGIN
    -- This will be populated by the sample data script
    RAISE NOTICE 'Use sample-dev-data.sql to generate test data';
END;
$$ LANGUAGE plpgsql;

COMMIT;
```

Create sample development data script (infrastructure/database/sample-dev-data.sql):
```sql
-- Sample Development Data
-- This script populates the database with sample data for development and testing

\c rag_app_dev;

-- Insert development users (these should match Keycloak users)
INSERT INTO users (user_id, username, email, first_name, last_name, role, keycloak_user_id) VALUES
('user-001', 'john.doe', 'john.doe@example.com', 'John', 'Doe', 'STANDARD', 'keycloak-user-001'),
('user-002', 'jane.admin', 'jane.admin@example.com', 'Jane', 'Admin', 'ADMIN', 'keycloak-user-002'),
('user-003', 'test.user', 'test.user@example.com', 'Test', 'User', 'STANDARD', 'keycloak-user-003'),
('user-004', 'demo.user', 'demo.user@example.com', 'Demo', 'User', 'STANDARD', 'keycloak-user-004')
ON CONFLICT (user_id) DO NOTHING;

-- Insert sample documents
INSERT INTO documents (document_id, file_name, file_size, file_type, uploaded_by, status, content_hash) VALUES
('doc-001', 'sample-guide.pdf', 1024000, 'PDF', 'user-001', 'READY', 'hash-001'),
('doc-002', 'project-readme.md', 5120, 'MARKDOWN', 'user-001', 'READY', 'hash-002'),
('doc-003', 'meeting-notes.txt', 2048, 'PLAIN_TEXT', 'user-003', 'READY', 'hash-003'),
('doc-004', 'large-document.pdf', 15000000, 'PDF', 'user-002', 'PROCESSING', 'hash-004'),
('doc-005', 'failed-upload.pdf', 512000, 'PDF', 'user-003', 'FAILED', 'hash-005')
ON CONFLICT (document_id) DO NOTHING;

-- Update processing timestamps for sample documents
UPDATE documents SET 
    processing_started_at = CURRENT_TIMESTAMP - INTERVAL '5 minutes',
    processing_completed_at = CURRENT_TIMESTAMP - INTERVAL '2 minutes'
WHERE status = 'READY';

UPDATE documents SET 
    processing_started_at = CURRENT_TIMESTAMP - INTERVAL '10 minutes'
WHERE status = 'PROCESSING';

UPDATE documents SET 
    processing_started_at = CURRENT_TIMESTAMP - INTERVAL '15 minutes',
    failure_reason = 'Corrupted PDF file - unable to extract text content'
WHERE status = 'FAILED';

-- Insert sample chat messages
INSERT INTO chat_messages (message_id, user_id, question, answer, response_time_ms) VALUES
('msg-001', 'user-001', 'What is the main topic of the sample guide?', 'The sample guide covers the basics of document management and provides step-by-step instructions for uploading and organizing files.', 1500),
('msg-002', 'user-001', 'How do I upload a document?', 'To upload a document, navigate to the document library and click the upload button. Select your file and wait for the processing to complete.', 1200),
('msg-003', 'user-003', 'What are the meeting notes about?', 'The meeting notes discuss the quarterly review and upcoming project milestones.', 800),
('msg-004', 'user-002', 'Show me all failed uploads', 'There is currently 1 failed upload: failed-upload.pdf uploaded by test.user. The failure reason is: Corrupted PDF file - unable to extract text content.', 2100)
ON CONFLICT (message_id) DO NOTHING;

-- Insert sample document references
INSERT INTO document_references (reference_id, message_id, document_id, document_name, paragraph_reference, relevance_score) VALUES
('ref-001', 'msg-001', 'doc-001', 'sample-guide.pdf', 'Introduction, paragraph 1', 0.95),
('ref-002', 'msg-002', 'doc-001', 'sample-guide.pdf', 'Chapter 2: File Upload, paragraph 3', 0.88),
('ref-003', 'msg-003', 'doc-003', 'meeting-notes.txt', 'Section 1: Quarterly Review', 0.92),
('ref-004', 'msg-004', 'doc-005', 'failed-upload.pdf', 'Error log entry', 0.99)
ON CONFLICT (reference_id) DO NOTHING;

-- Insert sample document chunks (simplified for development)
INSERT INTO document_chunks (chunk_id, document_id, chunk_index, text_content) VALUES
('chunk-001', 'doc-001', 1, 'This is the introduction to the sample guide. It covers the basics of document management.'),
('chunk-002', 'doc-001', 2, 'Chapter 2 explains how to upload files. Click the upload button and select your file.'),
('chunk-003', 'doc-002', 1, 'Project README: This project is a RAG application for document management and chat.'),
('chunk-004', 'doc-003', 1, 'Meeting Notes: Quarterly review discussion. Key points: budget approval, timeline updates.')
ON CONFLICT (chunk_id) DO NOTHING;

-- Create some sample user sessions
INSERT INTO user_sessions (session_id, user_id, expires_at) VALUES
('session-001', 'user-001', CURRENT_TIMESTAMP + INTERVAL '1 day'),
('session-002', 'user-002', CURRENT_TIMESTAMP + INTERVAL '1 day'),
('session-003', 'user-003', CURRENT_TIMESTAMP + INTERVAL '1 day')
ON CONFLICT (session_id) DO NOTHING;

-- Update statistics
ANALYZE users;
ANALYZE documents;
ANALYZE chat_messages;
ANALYZE document_references;
ANALYZE document_chunks;

-- Display summary
SELECT 'Development data loaded successfully' as status;
SELECT 'Users: ' || COUNT(*) as summary FROM users
UNION ALL
SELECT 'Documents: ' || COUNT(*) FROM documents
UNION ALL
SELECT 'Chat Messages: ' || COUNT(*) FROM chat_messages
UNION ALL
SELECT 'Document Chunks: ' || COUNT(*) FROM document_chunks;

COMMIT;
```

Create database management scripts:

Database reset script (infrastructure/database/reset-dev-db.sh):
```bash
#!/bin/bash
set -e

echo "=== Resetting Development Database ==="

# Database connection parameters
DB_HOST="localhost"
DB_PORT="5432"
DB_NAME="rag_app_dev"
DB_USER="rag_dev_user"
DB_PASSWORD="rag_dev_password"

# Check if PostgreSQL is running
if ! pg_isready -h $DB_HOST -p $DB_PORT -U $DB_USER > /dev/null 2>&1; then
    echo "ERROR: PostgreSQL is not running or not accessible"
    echo "Please start the development services first: ./start-dev-services.sh"
    exit 1
fi

echo "Clearing existing data..."
PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -c "SELECT reset_dev_data();"

echo "Reloading sample data..."
PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -f sample-dev-data.sql

echo "✓ Development database reset complete"
```

Database backup script (infrastructure/database/backup-dev-db.sh):
```bash
#!/bin/bash
set -e

BACKUP_DIR="./backups"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BACKUP_FILE="${BACKUP_DIR}/dev_backup_${TIMESTAMP}.sql"

# Create backup directory if it doesn't exist
mkdir -p $BACKUP_DIR

echo "=== Backing up Development Database ==="

# Database connection parameters
DB_HOST="localhost"
DB_PORT="5432"
DB_NAME="rag_app_dev"
DB_USER="rag_dev_user"
DB_PASSWORD="rag_dev_password"

# Create backup
PGPASSWORD=$DB_PASSWORD pg_dump -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME > $BACKUP_FILE

echo "✓ Database backup created: $BACKUP_FILE"
echo "File size: $(du -h $BACKUP_FILE | cut -f1)"
```

Database status script (infrastructure/database/status-dev-db.sh):
```bash
#!/bin/bash

echo "=== Development Database Status ==="

# Database connection parameters
DB_HOST="localhost"
DB_PORT="5432"
DB_NAME="rag_app_dev"
DB_USER="rag_dev_user"
DB_PASSWORD="rag_dev_password"

# Check connection
if pg_isready -h $DB_HOST -p $DB_PORT -U $DB_USER > /dev/null 2>&1; then
    echo "✓ PostgreSQL is running and accessible"
    
    # Get database statistics
    echo ""
    echo "Database Statistics:"
    PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -c "
        SELECT 
            schemaname,
            tablename,
            n_tup_ins as inserts,
            n_tup_upd as updates,
            n_tup_del as deletes,
            n_live_tup as live_rows
        FROM pg_stat_user_tables 
        ORDER BY tablename;
    "
    
    echo ""
    echo "User Document Summary:"
    PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -c "SELECT * FROM user_document_summary;"
    
    echo ""
    echo "Document Processing Status:"
    PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -c "SELECT * FROM document_processing_status;"
    
else
    echo "✗ PostgreSQL is not accessible"
    echo "Please start the development services: ./start-dev-services.sh"
fi
```

## Files / Modules Impacted

- infrastructure/database/init-dev.sql
- infrastructure/database/sample-dev-data.sql
- infrastructure/database/reset-dev-db.sh
- infrastructure/database/backup-dev-db.sh
- infrastructure/database/status-dev-db.sh
- docker-compose.dev.yml (volume mounts for scripts)

## Acceptance Criteria

Given the database initialization scripts are created
When PostgreSQL container starts
Then the database schema should be created automatically

Given sample data scripts are provided
When the database is initialized
Then development users and sample documents should be available

Given database management scripts are created
When developers need to reset or check database status
Then they should be able to use the provided scripts

Given the database is properly configured
When the application connects
Then it should be able to perform all CRUD operations

## Testing Requirements

- Test database schema creation and initialization
- Test sample data loading
- Test database reset and backup functionality
- Test database connection from application
- Test all database operations and constraints

## Dependencies / Preconditions

- PostgreSQL container must be running
- pgvector extension must be available
- Database user and permissions must be properly configured
- Network connectivity between application and database