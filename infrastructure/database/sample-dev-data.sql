-- Sample development data that mirrors the local Keycloak users.

INSERT INTO users (user_id, username, email, first_name, last_name, role, keycloak_user_id)
VALUES
    ('11111111-1111-1111-1111-111111111111', 'john.doe', 'john.doe@example.com', 'John', 'Doe', 'STANDARD', 'john.doe'),
    ('22222222-2222-2222-2222-222222222222', 'jane.admin', 'jane.admin@example.com', 'Jane', 'Admin', 'ADMIN', 'jane.admin'),
    ('33333333-3333-3333-3333-333333333333', 'test.user', 'test.user@example.com', 'Test', 'User', 'STANDARD', 'test.user'),
    ('44444444-4444-4444-4444-444444444444', 'demo.user', 'demo.user@example.com', 'Demo', 'User', 'STANDARD', 'demo.user')
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO documents (document_id, file_name, file_size, file_type, uploaded_by, status, content_hash)
VALUES
    ('00000000-0000-0000-0000-000000000001', 'sample-guide.pdf', 1024000, 'PDF', '11111111-1111-1111-1111-111111111111', 'READY', 'hash-001'),
    ('00000000-0000-0000-0000-000000000002', 'project-readme.md', 5120, 'MARKDOWN', '11111111-1111-1111-1111-111111111111', 'READY', 'hash-002'),
    ('00000000-0000-0000-0000-000000000003', 'meeting-notes.txt', 2048, 'PLAIN_TEXT', '33333333-3333-3333-3333-333333333333', 'READY', 'hash-003'),
    ('00000000-0000-0000-0000-000000000004', 'large-document.pdf', 15000000, 'PDF', '22222222-2222-2222-2222-222222222222', 'READY', 'hash-004'),
    ('00000000-0000-0000-0000-000000000005', 'failed-upload.pdf', 512000, 'PDF', '33333333-3333-3333-3333-333333333333', 'FAILED', 'hash-005')
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
    ('10000000-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111', 'What is the main topic of the sample guide?', 'The sample guide covers the basics of document management and provides step-by-step instructions for uploading and organizing files.', 1500),
    ('10000000-0000-0000-0000-000000000002', '11111111-1111-1111-1111-111111111111', 'How do I upload a document?', 'Navigate to the document library, click upload, choose a file, and wait for processing to finish.', 1200),
    ('10000000-0000-0000-0000-000000000003', '33333333-3333-3333-3333-333333333333', 'What are the meeting notes about?', 'The meeting notes discuss the quarterly review and upcoming project milestones.', 800),
    ('10000000-0000-0000-0000-000000000004', '22222222-2222-2222-2222-222222222222', 'Show me all failed uploads', 'There is currently 1 failed upload: failed-upload.pdf uploaded by test.user.', 2100)
ON CONFLICT (message_id) DO NOTHING;

INSERT INTO document_references (reference_id, message_id, document_id, document_name, paragraph_reference, relevance_score)
VALUES
    ('ref-001', '10000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001', 'sample-guide.pdf', 'Introduction, paragraph 1', 0.95),
    ('ref-002', '10000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000001', 'sample-guide.pdf', 'Chapter 2: File Upload, paragraph 3', 0.88),
    ('ref-003', '10000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000003', 'meeting-notes.txt', 'Section 1: Quarterly Review', 0.92),
    ('ref-004', '10000000-0000-0000-0000-000000000004', '00000000-0000-0000-0000-000000000005', 'failed-upload.pdf', 'Error log entry', 0.99)
ON CONFLICT (reference_id) DO NOTHING;

INSERT INTO document_chunks (chunk_id, document_id, chunk_index, text_content)
VALUES
    ('chunk-001', '00000000-0000-0000-0000-000000000001', 1, 'This is the introduction to the sample guide. It covers the basics of document management.'),
    ('chunk-002', '00000000-0000-0000-0000-000000000001', 2, 'Chapter 2 explains how to upload files. Click the upload button and select your file.'),
    ('chunk-003', '00000000-0000-0000-0000-000000000002', 1, 'Project README: This project is a RAG application for document management and chat.'),
    ('chunk-004', '00000000-0000-0000-0000-000000000003', 1, 'Meeting Notes: Quarterly review discussion. Key points include budget approval and timeline updates.')
ON CONFLICT (chunk_id) DO NOTHING;

INSERT INTO user_sessions (session_id, user_id, expires_at)
VALUES
    ('session-001', '11111111-1111-1111-1111-111111111111', CURRENT_TIMESTAMP + INTERVAL '1 day'),
    ('session-002', '22222222-2222-2222-2222-222222222222', CURRENT_TIMESTAMP + INTERVAL '1 day'),
    ('session-003', '33333333-3333-3333-3333-333333333333', CURRENT_TIMESTAMP + INTERVAL '1 day')
ON CONFLICT (session_id) DO NOTHING;

ANALYZE users;
ANALYZE documents;
ANALYZE chat_messages;
ANALYZE document_references;
ANALYZE document_chunks;
ANALYZE user_sessions;
