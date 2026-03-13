package com.rag.app.infrastructure.persistence;

import com.rag.app.domain.entities.Document;
import com.rag.app.domain.valueobjects.DocumentMetadata;
import com.rag.app.domain.valueobjects.DocumentStatus;
import com.rag.app.domain.valueobjects.FileType;
import com.rag.app.usecases.models.ProcessingStatistics;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcDocumentRepositoryTest {
    private JdbcDocumentRepository repository;

    @BeforeEach
    void setUp() throws SQLException {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:documents;DB_CLOSE_DELAY=-1;MODE=PostgreSQL");
        dataSource.setUser("sa");
        dataSource.setPassword("sa");

        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS documents");
            statement.execute("""
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
                    processing_started_at TIMESTAMP
                )
                """);
        }

        repository = new JdbcDocumentRepository(dataSource, new DocumentRowMapper());
    }

    @Test
    void shouldSaveAndFindDocumentById() {
        Document document = document("guide.pdf", "user-1", DocumentStatus.UPLOADED, "hash-1");

        repository.save(document);

        Document saved = repository.findById(document.documentId()).orElseThrow();
        assertEquals(document.documentId(), saved.documentId());
        assertEquals("guide.pdf", saved.fileName());
        assertEquals("user-1", saved.uploadedBy());
    }

    @Test
    void shouldFindDocumentsByUploadedByAndStatus() {
        Document ownedReady = document("owned-ready.pdf", "user-1", DocumentStatus.READY, "hash-2");
        Document ownedFailed = document("owned-failed.pdf", "user-1", DocumentStatus.FAILED, "hash-3");
        Document otherReady = document("other-ready.pdf", "user-2", DocumentStatus.READY, "hash-4");
        repository.save(ownedReady);
        repository.save(ownedFailed);
        repository.save(otherReady);

        List<Document> byUser = repository.findByUploadedBy("user-1");
        List<Document> ready = repository.findByStatus(DocumentStatus.READY);

        assertEquals(2, byUser.size());
        assertEquals(2, ready.size());
        assertTrue(ready.stream().anyMatch(document -> document.documentId().equals(ownedReady.documentId())));
    }

    @Test
    void shouldFindByContentHashAndReturnStatistics() {
        repository.save(document("uploaded.pdf", "user-1", DocumentStatus.UPLOADED, "hash-5"));
        repository.save(document("processing.pdf", "user-1", DocumentStatus.PROCESSING, "hash-6"));
        repository.save(document("ready.pdf", "user-1", DocumentStatus.READY, "hash-7"));
        repository.save(document("failed.pdf", "user-1", DocumentStatus.FAILED, "hash-8"));

        Document found = repository.findByContentHash("hash-7").orElseThrow();
        ProcessingStatistics statistics = repository.getProcessingStatistics();

        assertEquals("ready.pdf", found.fileName());
        assertEquals(new ProcessingStatistics(4, 1, 1, 1, 1), statistics);
    }

    @Test
    void shouldReturnFailedAndProcessingDocuments() {
        Document failed = document("failed.pdf", "user-1", DocumentStatus.FAILED, "hash-9");
        Document processing = document("processing.pdf", "user-2", DocumentStatus.PROCESSING, "hash-10");
        repository.save(failed);
        repository.save(processing);

        assertEquals(1, repository.findFailedDocuments().size());
        assertEquals("failed.pdf", repository.findFailedDocuments().get(0).fileName());
        assertEquals(1, repository.findProcessingDocuments().size());
        assertEquals("processing.pdf", repository.findProcessingDocuments().get(0).fileName());
        assertNotNull(repository.findProcessingDocuments().get(0).processingStartedAt());
    }

    @Test
    void shouldUpdateStatus() {
        Document uploaded = document("status.pdf", "user-1", DocumentStatus.UPLOADED, "hash-11");
        repository.save(uploaded);

        repository.updateStatus(uploaded.documentId(), DocumentStatus.PROCESSING);

        assertEquals(DocumentStatus.PROCESSING, repository.findById(uploaded.documentId()).orElseThrow().status());
    }

    private static Document document(String fileName, String uploadedBy, DocumentStatus status, String contentHash) {
        return new Document(
            UUID.randomUUID(),
            new DocumentMetadata(fileName, 128L, FileType.PDF, contentHash),
            uploadedBy,
            Instant.parse("2026-03-13T08:00:00Z"),
            status
        );
    }
}
