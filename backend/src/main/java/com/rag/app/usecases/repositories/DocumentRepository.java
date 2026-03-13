package com.rag.app.usecases.repositories;

import com.rag.app.domain.entities.Document;
import com.rag.app.domain.valueobjects.DocumentStatus;
import com.rag.app.usecases.models.FailedDocumentInfo;
import com.rag.app.usecases.models.ProcessingDocumentInfo;
import com.rag.app.usecases.models.ProcessingStatistics;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentRepository {
    Document save(Document document);

    Optional<Document> findByContentHash(String hash);

    Optional<Document> findById(UUID documentId);

    List<Document> findByUploadedBy(String userId);

    List<Document> findAll();

    List<Document> findByStatus(DocumentStatus status);

    ProcessingStatistics getProcessingStatistics();

    List<FailedDocumentInfo> findFailedDocuments();

    List<ProcessingDocumentInfo> findProcessingDocuments();

    void updateStatus(UUID documentId, DocumentStatus status);
}
