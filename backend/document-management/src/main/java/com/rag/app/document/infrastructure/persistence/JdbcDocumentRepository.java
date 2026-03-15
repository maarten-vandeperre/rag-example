package com.rag.app.document.infrastructure.persistence;

import com.rag.app.document.domain.entities.Document;
import com.rag.app.document.domain.valueobjects.DocumentStatus;
import com.rag.app.document.interfaces.DocumentRepository;
import com.rag.app.document.usecases.models.FailedDocumentInfo;
import com.rag.app.document.usecases.models.ProcessingDocumentInfo;
import com.rag.app.document.usecases.models.ProcessingStatistics;

import javax.sql.DataSource;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class JdbcDocumentRepository implements DocumentRepository {
    private final DataSource dataSource;

    public JdbcDocumentRepository(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
    }

    @Override
    public Document save(Document document) {
        throw new UnsupportedOperationException("JdbcDocumentRepository persistence wiring is not yet connected in this module");
    }

    @Override
    public Optional<Document> findByContentHash(String hash) {
        return Optional.empty();
    }

    @Override
    public Optional<Document> findById(UUID documentId) {
        return Optional.empty();
    }

    @Override
    public List<Document> findByUploadedBy(String userId) {
        return List.of();
    }

    @Override
    public List<Document> findAll() {
        return List.of();
    }

    @Override
    public List<Document> findByStatus(DocumentStatus status) {
        return List.of();
    }

    @Override
    public ProcessingStatistics getProcessingStatistics() {
        return new ProcessingStatistics(0, 0, 0, 0, 0);
    }

    @Override
    public List<FailedDocumentInfo> findFailedDocuments() {
        return List.of();
    }

    @Override
    public List<ProcessingDocumentInfo> findProcessingDocuments() {
        return List.of();
    }

    public DataSource dataSource() {
        return dataSource;
    }
}
