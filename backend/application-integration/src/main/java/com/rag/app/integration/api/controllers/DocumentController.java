package com.rag.app.integration.api.controllers;

import com.rag.app.document.interfaces.DocumentManagementFacade;
import com.rag.app.document.usecases.models.GetUserDocumentsInput;
import com.rag.app.document.usecases.models.GetUserDocumentsOutput;
import com.rag.app.document.usecases.models.UploadDocumentInput;
import com.rag.app.document.usecases.models.UploadDocumentOutput;
import com.rag.app.integration.api.dto.ApiResponse;
import com.rag.app.integration.events.events.DocumentUploadedEvent;
import com.rag.app.integration.orchestration.ApplicationOrchestrator;

public final class DocumentController {
    private final ApplicationOrchestrator orchestrator;
    private final DocumentManagementFacade documentManagement;

    public DocumentController(ApplicationOrchestrator orchestrator, DocumentManagementFacade documentManagement) {
        this.orchestrator = orchestrator;
        this.documentManagement = documentManagement;
    }

    public ApiResponse<UploadDocumentOutput> uploadDocument(UploadDocumentInput input) {
        try {
            UploadDocumentOutput result = documentManagement.uploadDocument(input);
            orchestrator.publishEvent(new DocumentUploadedEvent(result.documentId().toString(), input.fileName(), input.uploadedBy()));
            return ApiResponse.success(result);
        } catch (RuntimeException exception) {
            return ApiResponse.failure("DOCUMENT_UPLOAD_FAILED", exception.getMessage());
        }
    }

    public ApiResponse<GetUserDocumentsOutput> getUserDocuments(String userId, boolean includeAll) {
        return ApiResponse.success(documentManagement.getUserDocuments(new GetUserDocumentsInput(userId, includeAll)));
    }
}
