-- Sample development data that mirrors the local Keycloak users.

INSERT INTO users (user_id, username, email, first_name, last_name, role, keycloak_user_id)
VALUES
    ('user-001', 'john.doe', 'john.doe@example.com', 'John', 'Doe', 'STANDARD', 'john.doe'),
    ('user-002', 'jane.admin', 'jane.admin@example.com', 'Jane', 'Admin', 'ADMIN', 'jane.admin'),
    ('user-003', 'test.user', 'test.user@example.com', 'Test', 'User', 'STANDARD', 'test.user'),
    ('user-004', 'demo.user', 'demo.user@example.com', 'Demo', 'User', 'STANDARD', 'demo.user')
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO documents (document_id, file_name, file_size, file_type, uploaded_by, status, content_hash)
VALUES
    ('doc-001', 'sample-guide.pdf', 1024000, 'PDF', 'user-001', 'READY', 'hash-001'),
    ('doc-002', 'project-readme.md', 5120, 'MARKDOWN', 'user-001', 'READY', 'hash-002'),
    ('doc-003', 'meeting-notes.txt', 2048, 'PLAIN_TEXT', 'user-003', 'READY', 'hash-003'),
    ('doc-004', 'large-document.pdf', 15000000, 'PDF', 'user-002', 'PROCESSING', 'hash-004'),
    ('doc-005', 'failed-upload.pdf', 512000, 'PDF', 'user-003', 'FAILED', 'hash-005')
ON CONFLICT (document_id) DO NOTHING;

UPDATE documents
SET processing_started_at = CURRENT_TIMESTAMP - INTERVAL '5 minutes',
    processing_completed_at = CURRENT_TIMESTAMP - INTERVAL '2 minutes'
WHERE status = 'READY';

UPDATE documents
SET processing_started_at = CURRENT_TIMESTAMP - INTERVAL '10 minutes'
WHERE status = 'PROCESSING';

UPDATE documents
SET processing_started_at = CURRENT_TIMESTAMP - INTERVAL '15 minutes',
    failure_reason = 'Corrupted PDF file - unable to extract text content'
WHERE status = 'FAILED';

INSERT INTO chat_messages (message_id, user_id, question, answer, response_time_ms)
VALUES
    ('msg-001', 'user-001', 'What is the main topic of the sample guide?', 'The sample guide covers the basics of document management and provides step-by-step instructions for uploading and organizing files.', 1500),
    ('msg-002', 'user-001', 'How do I upload a document?', 'Navigate to the document library, click upload, choose a file, and wait for processing to finish.', 1200),
    ('msg-003', 'user-003', 'What are the meeting notes about?', 'The meeting notes discuss the quarterly review and upcoming project milestones.', 800),
    ('msg-004', 'user-002', 'Show me all failed uploads', 'There is currently 1 failed upload: failed-upload.pdf uploaded by test.user.', 2100)
ON CONFLICT (message_id) DO NOTHING;

INSERT INTO document_references (reference_id, message_id, document_id, document_name, paragraph_reference, relevance_score)
VALUES
    ('ref-001', 'msg-001', 'doc-001', 'sample-guide.pdf', 'Introduction, paragraph 1', 0.95),
    ('ref-002', 'msg-002', 'doc-001', 'sample-guide.pdf', 'Chapter 2: File Upload, paragraph 3', 0.88),
    ('ref-003', 'msg-003', 'doc-003', 'meeting-notes.txt', 'Section 1: Quarterly Review', 0.92),
    ('ref-004', 'msg-004', 'doc-005', 'failed-upload.pdf', 'Error log entry', 0.99)
ON CONFLICT (reference_id) DO NOTHING;

INSERT INTO document_chunks (chunk_id, document_id, chunk_index, text_content)
VALUES
    ('chunk-001', 'doc-001', 1, 'This is the introduction to the sample guide. It covers the basics of document management.'),
    ('chunk-002', 'doc-001', 2, 'Chapter 2 explains how to upload files. Click the upload button and select your file.'),
    ('chunk-003', 'doc-002', 1, 'Project README: This project is a RAG application for document management and chat.'),
    ('chunk-004', 'doc-003', 1, 'Meeting Notes: Quarterly review discussion. Key points include budget approval and timeline updates.')
ON CONFLICT (chunk_id) DO NOTHING;

INSERT INTO user_sessions (session_id, user_id, expires_at)
VALUES
    ('session-001', 'user-001', CURRENT_TIMESTAMP + INTERVAL '1 day'),
    ('session-002', 'user-002', CURRENT_TIMESTAMP + INTERVAL '1 day'),
    ('session-003', 'user-003', CURRENT_TIMESTAMP + INTERVAL '1 day')
ON CONFLICT (session_id) DO NOTHING;

ANALYZE users;
ANALYZE documents;
ANALYZE chat_messages;
ANALYZE document_references;
ANALYZE document_chunks;
ANALYZE user_sessions;
