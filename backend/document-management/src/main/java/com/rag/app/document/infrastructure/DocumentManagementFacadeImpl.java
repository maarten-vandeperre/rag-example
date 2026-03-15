package com.rag.app.document.infrastructure;

import com.rag.app.document.domain.entities.Document;
import com.rag.app.document.interfaces.DocumentManagementFacade;
import com.rag.app.document.interfaces.DocumentRepository;
import com.rag.app.document.usecases.GetAdminProgress;
import com.rag.app.document.usecases.GetUserDocuments;
import com.rag.app.document.usecases.ProcessDocument;
import com.rag.app.document.usecases.UploadDocument;
import com.rag.app.document.usecases.models.GetAdminProgressInput;
import com.rag.app.document.usecases.models.GetAdminProgressOutput;
import com.rag.app.document.usecases.models.GetUserDocumentsInput;
import com.rag.app.document.usecases.models.GetUserDocumentsOutput;
import com.rag.app.document.usecases.models.ProcessDocumentInput;
import com.rag.app.document.usecases.models.ProcessDocumentOutput;
import com.rag.app.document.usecases.models.UploadDocumentInput;
import com.rag.app.document.usecases.models.UploadDocumentOutput;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class DocumentManagementFacadeImpl implements DocumentManagementFacade {
    private final UploadDocument uploadDocument;
    private final ProcessDocument processDocument;
    private final GetUserDocuments getUserDocuments;
    private final GetAdminProgress getAdminProgress;
    private final DocumentRepository documentRepository;

    public DocumentManagementFacadeImpl(UploadDocument uploadDocument,
                                        ProcessDocument processDocument,
                                        GetUserDocuments getUserDocuments,
                                        GetAdminProgress getAdminProgress,
                                        DocumentRepository documentRepository) {
        this.uploadDocument = Objects.requireNonNull(uploadDocument, "uploadDocument must not be null");
        this.processDocument = Objects.requireNonNull(processDocument, "processDocument must not be null");
        this.getUserDocuments = Objects.requireNonNull(getUserDocuments, "getUserDocuments must not be null");
        this.getAdminProgress = Objects.requireNonNull(getAdminProgress, "getAdminProgress must not be null");
        this.documentRepository = Objects.requireNonNull(documentRepository, "documentRepository must not be null");
    }

    @Override
    public UploadDocumentOutput uploadDocument(UploadDocumentInput input) {
        return uploadDocument.execute(input);
    }

    @Override
    public ProcessDocumentOutput processDocument(ProcessDocumentInput input) {
        return processDocument.execute(input);
    }

    @Override
    public GetUserDocumentsOutput getUserDocuments(GetUserDocumentsInput input) {
        return getUserDocuments.execute(input);
    }

    @Override
    public GetAdminProgressOutput getAdminProgress(GetAdminProgressInput input) {
        return getAdminProgress.execute(input);
    }

    @Override
    public Optional<Document> findDocumentById(String documentId) {
        return documentRepository.findById(UUID.fromString(documentId));
    }

    @Override
    public List<Document> findDocumentsByUser(String userId) {
        return documentRepository.findByUploadedBy(userId);
    }
}
