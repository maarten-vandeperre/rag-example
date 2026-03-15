package com.rag.app.infrastructure.persistence;

import com.rag.app.domain.entities.Document;
import com.rag.app.domain.valueobjects.DocumentStatus;
import com.rag.app.usecases.models.FailedDocumentInfo;
import com.rag.app.usecases.models.ProcessingDocumentInfo;
import com.rag.app.usecases.models.ProcessingStatistics;
import com.rag.app.usecases.repositories.DocumentRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class JdbcDocumentRepository implements DocumentRepository {
    private static final String INSERT_SQL = """
        INSERT INTO documents (
            document_id, file_name, file_size, file_type, uploaded_by, uploaded_at,
            status, content_hash, last_updated, failure_reason, processing_started_at
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
    private static final String UPDATE_SQL = """
        UPDATE documents
        SET file_name = ?,
            file_size = ?,
            file_type = ?,
            uploaded_by = ?,
            uploaded_at = ?,
            status = ?,
            content_hash = ?,
            last_updated = ?,
            failure_reason = ?,
            processing_started_at = ?
        WHERE document_id = ?
        """;

    private final DataSource dataSource;
    private final DocumentRowMapper documentRowMapper;

    @Inject
    public JdbcDocumentRepository(DataSource dataSource, DocumentRowMapper documentRowMapper) {
        this.dataSource = dataSource;
        this.documentRowMapper = documentRowMapper;
    }

    @Override
    public Document save(Document document) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement updateStatement = connection.prepareStatement(UPDATE_SQL)) {
            RowState rowState = existingRowState(document.documentId()).orElse(new RowState(null, null));
            bindUpdate(updateStatement, document, rowState.processingStartedAt(), rowState.failureReason());

            if (updateStatement.executeUpdate() == 0) {
                try (PreparedStatement insertStatement = connection.prepareStatement(INSERT_SQL)) {
                    bindInsert(insertStatement, document, rowState.processingStartedAt(), rowState.failureReason());
                    insertStatement.executeUpdate();
                }
            }

            return document;
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to save document", exception);
        }
    }

    @Override
    public Optional<Document> findByContentHash(String hash) {
        return findSingle("SELECT * FROM documents WHERE content_hash = ?", hash);
    }

    @Override
    public Optional<Document> findById(UUID documentId) {
        return findSingle("SELECT * FROM documents WHERE document_id = ?", documentId.toString());
    }

    @Override
    public List<Document> findByUploadedBy(String userId) {
        return findMany("SELECT * FROM documents WHERE uploaded_by = ? ORDER BY uploaded_at DESC", userId);
    }

    @Override
    public List<Document> findAll() {
        return findMany("SELECT * FROM documents ORDER BY uploaded_at DESC");
    }

    @Override
    public List<Document> findByStatus(DocumentStatus status) {
        return findMany("SELECT * FROM documents WHERE status = ? ORDER BY uploaded_at DESC", status.name());
    }

    @Override
    public ProcessingStatistics getProcessingStatistics() {
        String sql = """
            SELECT COUNT(*) AS total_documents,
                   COALESCE(SUM(CASE WHEN status = 'UPLOADED' THEN 1 ELSE 0 END), 0) AS uploaded_count,
                   COALESCE(SUM(CASE WHEN status = 'PROCESSING' THEN 1 ELSE 0 END), 0) AS processing_count,
                   COALESCE(SUM(CASE WHEN status = 'READY' THEN 1 ELSE 0 END), 0) AS ready_count,
                   COALESCE(SUM(CASE WHEN status = 'FAILED' THEN 1 ELSE 0 END), 0) AS failed_count
            FROM documents
            """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            return new ProcessingStatistics(
                resultSet.getInt("total_documents"),
                resultSet.getInt("uploaded_count"),
                resultSet.getInt("processing_count"),
                resultSet.getInt("ready_count"),
                resultSet.getInt("failed_count")
            );
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to fetch processing statistics", exception);
        }
    }

    @Override
    public List<FailedDocumentInfo> findFailedDocuments() {
        String sql = "SELECT * FROM documents WHERE status = ? ORDER BY uploaded_at DESC";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, DocumentStatus.FAILED.name());
            try (ResultSet resultSet = statement.executeQuery()) {
                List<FailedDocumentInfo> failedDocuments = new ArrayList<>();
                while (resultSet.next()) {
                    DocumentRowMapper.FailedDocumentRecord record = documentRowMapper.mapFailedDocument(resultSet);
                    failedDocuments.add(new FailedDocumentInfo(
                        record.documentId(),
                        record.fileName(),
                        record.uploadedBy(),
                        record.uploadedAt(),
                        record.failureReason(),
                        record.fileSize()
                    ));
                }
                return failedDocuments;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to fetch failed documents", exception);
        }
    }

    @Override
    public List<ProcessingDocumentInfo> findProcessingDocuments() {
        String sql = "SELECT * FROM documents WHERE status = ? ORDER BY processing_started_at ASC";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, DocumentStatus.PROCESSING.name());
            try (ResultSet resultSet = statement.executeQuery()) {
                List<ProcessingDocumentInfo> processingDocuments = new ArrayList<>();
                while (resultSet.next()) {
                    DocumentRowMapper.ProcessingDocumentRecord record = documentRowMapper.mapProcessingDocument(resultSet);
                    processingDocuments.add(new ProcessingDocumentInfo(
                        record.documentId(),
                        record.fileName(),
                        record.uploadedBy(),
                        record.uploadedAt(),
                        record.processingStartedAt()
                    ));
                }
                return processingDocuments;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to fetch processing documents", exception);
        }
    }

    @Override
    public void updateStatus(UUID documentId, DocumentStatus status) {
        String sql = """
            UPDATE documents
            SET status = ?, last_updated = ?, processing_started_at = ?, failure_reason = ?
            WHERE document_id = ?
            """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            Instant now = Instant.now();
            statement.setString(1, status.name());
            statement.setTimestamp(2, Timestamp.from(now));
            statement.setTimestamp(3, status == DocumentStatus.PROCESSING ? Timestamp.from(now) : null);
            statement.setString(4, null);
            statement.setString(5, documentId.toString());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to update document status", exception);
        }
    }

    private Optional<Document> findSingle(String sql, String parameter) {
        List<Document> documents = findMany(sql, parameter);
        return documents.stream().findFirst();
    }

    private List<Document> findMany(String sql, String... parameters) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int index = 0; index < parameters.length; index++) {
                statement.setString(index + 1, parameters[index]);
            }

            try (ResultSet resultSet = statement.executeQuery()) {
                List<Document> documents = new ArrayList<>();
                while (resultSet.next()) {
                    documents.add(documentRowMapper.map(resultSet));
                }
                return documents;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to query documents", exception);
        }
    }

    private Optional<RowState> existingRowState(UUID documentId) {
        String sql = "SELECT processing_started_at, failure_reason FROM documents WHERE document_id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, documentId.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                Timestamp processingStartedAt = resultSet.getTimestamp("processing_started_at");
                return Optional.of(new RowState(
                    processingStartedAt == null ? null : processingStartedAt.toInstant(),
                    resultSet.getString("failure_reason")
                ));
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to fetch existing document state", exception);
        }
    }

    private void bindUpdate(PreparedStatement statement,
                            Document document,
                            Instant existingProcessingStartedAt,
                            String existingFailureReason) throws SQLException {
        statement.setString(1, document.fileName());
        statement.setLong(2, document.fileSize());
        statement.setString(3, document.fileType().name());
        statement.setString(4, document.uploadedBy());
        statement.setTimestamp(5, Timestamp.from(document.uploadedAt()));
        statement.setString(6, document.status().name());
        statement.setString(7, document.contentHash());
        statement.setTimestamp(8, Timestamp.from(Instant.now()));
        statement.setString(9, document.status() == DocumentStatus.FAILED ? existingFailureReason : null);
        statement.setTimestamp(10, processingStartedAt(document.status(), existingProcessingStartedAt));
        statement.setString(11, document.documentId().toString());
    }

    private void bindInsert(PreparedStatement statement,
                            Document document,
                            Instant existingProcessingStartedAt,
                            String existingFailureReason) throws SQLException {
        statement.setString(1, document.documentId().toString());
        statement.setString(2, document.fileName());
        statement.setLong(3, document.fileSize());
        statement.setString(4, document.fileType().name());
        statement.setString(5, document.uploadedBy());
        statement.setTimestamp(6, Timestamp.from(document.uploadedAt()));
        statement.setString(7, document.status().name());
        statement.setString(8, document.contentHash());
        statement.setTimestamp(9, Timestamp.from(Instant.now()));
        statement.setString(10, document.status() == DocumentStatus.FAILED ? existingFailureReason : null);
        statement.setTimestamp(11, processingStartedAt(document.status(), existingProcessingStartedAt));
    }

    private Timestamp processingStartedAt(DocumentStatus status, Instant existingProcessingStartedAt) {
        if (status != DocumentStatus.PROCESSING) {
            return null;
        }
        Instant value = existingProcessingStartedAt == null ? Instant.now() : existingProcessingStartedAt;
        return Timestamp.from(value);
    }

    private record RowState(Instant processingStartedAt, String failureReason) {
    }
}
