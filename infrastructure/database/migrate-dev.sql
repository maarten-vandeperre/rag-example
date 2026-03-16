ALTER TABLE document_references
    ADD COLUMN IF NOT EXISTS reference_index INTEGER NOT NULL DEFAULT 0;

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
        FOREIGN KEY (answer_id) REFERENCES chat_messages(message_id) ON DELETE CASCADE,
    CONSTRAINT fk_answer_source_references_document_id
        FOREIGN KEY (document_id) REFERENCES documents(document_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_answer_source_references_answer_id
    ON answer_source_references(answer_id, source_order);
