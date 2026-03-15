package com.rag.app.infrastructure.persistence;

import com.rag.app.domain.entities.Document;
import com.rag.app.domain.valueobjects.DocumentMetadata;
import com.rag.app.domain.valueobjects.DocumentStatus;
import com.rag.app.domain.valueobjects.FileType;

import jakarta.enterprise.context.ApplicationScoped;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

@ApplicationScoped
public final class DocumentRowMapper {

    public Document map(ResultSet resultSet) throws SQLException {
        return new Document(
            UUID.fromString(resultSet.getString("document_id")),
            new DocumentMetadata(
                resultSet.getString("file_name"),
                resultSet.getLong("file_size"),
                FileType.valueOf(resultSet.getString("file_type")),
                resultSet.getString("content_hash")
            ),
            resultSet.getString("uploaded_by"),
            toInstant(resultSet.getTimestamp("uploaded_at")),
            DocumentStatus.valueOf(resultSet.getString("status"))
        );
    }

    public FailedDocumentRecord mapFailedDocument(ResultSet resultSet) throws SQLException {
        return new FailedDocumentRecord(
            resultSet.getString("document_id"),
            resultSet.getString("file_name"),
            resultSet.getString("uploaded_by"),
            toInstant(resultSet.getTimestamp("uploaded_at")),
            resultSet.getString("failure_reason"),
            resultSet.getLong("file_size")
        );
    }

    public ProcessingDocumentRecord mapProcessingDocument(ResultSet resultSet) throws SQLException {
        return new ProcessingDocumentRecord(
            resultSet.getString("document_id"),
            resultSet.getString("file_name"),
            resultSet.getString("uploaded_by"),
            toInstant(resultSet.getTimestamp("uploaded_at")),
            toInstant(resultSet.getTimestamp("processing_started_at"))
        );
    }

    private Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    public record FailedDocumentRecord(String documentId,
                                       String fileName,
                                       String uploadedBy,
                                       Instant uploadedAt,
                                       String failureReason,
                                       long fileSize) {
    }

    public record ProcessingDocumentRecord(String documentId,
                                           String fileName,
                                           String uploadedBy,
                                           Instant uploadedAt,
                                           Instant processingStartedAt) {
    }
}
