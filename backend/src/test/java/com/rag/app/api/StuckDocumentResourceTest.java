package com.rag.app.api;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.core.Response;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class StuckDocumentResourceTest {
    private JdbcDataSource dataSource;
    private StuckDocumentResource resource;

    @BeforeEach
    void setUp() throws SQLException {
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:stuck-documents-" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
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

        resource = new StuckDocumentResource(dataSource);
    }

    @Test
    void shouldListUploadedDocumentsAsStuck() throws SQLException {
        String stuckId = UUID.randomUUID().toString();
        insertDocument(stuckId, "guide.pdf", "UPLOADED", Instant.parse("2026-03-16T09:00:00Z"));
        insertDocument(UUID.randomUUID().toString(), "ready.pdf", "READY", Instant.parse("2026-03-16T09:30:00Z"));

        Response response = resource.getStuckDocuments();

        assertEquals(200, response.getStatus());
        StuckDocumentResource.StuckDocumentsResponse entity = assertInstanceOf(
            StuckDocumentResource.StuckDocumentsResponse.class,
            response.getEntity()
        );
        assertEquals(1, entity.documents().size());
        assertEquals(stuckId, entity.documents().get(0).documentId());
        assertEquals("guide.pdf", entity.documents().get(0).fileName());
    }

    @Test
    void shouldMarkSingleUploadedDocumentAsFailed() throws SQLException {
        String stuckId = UUID.randomUUID().toString();
        insertDocument(stuckId, "single.pdf", "UPLOADED", Instant.parse("2026-03-16T10:00:00Z"));

        Response response = resource.markDocumentAsFailed(stuckId);

        assertEquals(200, response.getStatus());
        StuckDocumentResource.CleanupResponse entity = assertInstanceOf(StuckDocumentResource.CleanupResponse.class, response.getEntity());
        assertEquals(1, entity.updatedCount());
        assertEquals(List.of(stuckId), entity.documentIds());
        assertEquals("FAILED", loadStatus(stuckId));
        assertEquals(
            "Document upload became stuck before processing completed. Please re-upload the document to try again.",
            loadFailureReason(stuckId)
        );
    }

    @Test
    void shouldCleanupAllUploadedDocuments() throws SQLException {
        String firstId = UUID.randomUUID().toString();
        String secondId = UUID.randomUUID().toString();
        insertDocument(firstId, "first.pdf", "UPLOADED", Instant.parse("2026-03-16T08:00:00Z"));
        insertDocument(secondId, "second.pdf", "UPLOADED", Instant.parse("2026-03-16T08:30:00Z"));
        insertDocument(UUID.randomUUID().toString(), "ready.pdf", "READY", Instant.parse("2026-03-16T09:00:00Z"));

        Response response = resource.cleanupStuckDocuments();

        assertEquals(200, response.getStatus());
        StuckDocumentResource.CleanupResponse entity = assertInstanceOf(StuckDocumentResource.CleanupResponse.class, response.getEntity());
        assertEquals(2, entity.updatedCount());
        assertEquals(List.of(firstId, secondId), entity.documentIds());
        assertEquals("FAILED", loadStatus(firstId));
        assertEquals("FAILED", loadStatus(secondId));
    }

    private void insertDocument(String documentId, String fileName, String status, Instant uploadedAt) throws SQLException {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("""
                INSERT INTO documents (document_id, file_name, file_size, file_type, uploaded_by, uploaded_at, status, content_hash, last_updated, failure_reason, processing_started_at)
                VALUES ('%s', '%s', 128, 'PDF', 'user-1', TIMESTAMP '%s', '%s', '%s-hash', TIMESTAMP '%s', NULL, NULL)
                """.formatted(documentId, fileName, uploadedAt, status, documentId, uploadedAt));
        }
    }

    private String loadStatus(String documentId) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             var resultSet = statement.executeQuery("SELECT status FROM documents WHERE document_id = '" + documentId + "'")) {
            resultSet.next();
            return resultSet.getString("status");
        }
    }

    private String loadFailureReason(String documentId) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             var resultSet = statement.executeQuery("SELECT failure_reason FROM documents WHERE document_id = '" + documentId + "'")) {
            resultSet.next();
            return resultSet.getString("failure_reason");
        }
    }
}
