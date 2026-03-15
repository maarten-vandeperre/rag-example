package com.rag.app.document.interfaces;

import com.rag.app.document.domain.entities.Document;
import com.rag.app.document.usecases.models.GetAdminProgressInput;
import com.rag.app.document.usecases.models.GetAdminProgressOutput;
import com.rag.app.document.usecases.models.GetUserDocumentsInput;
import com.rag.app.document.usecases.models.GetUserDocumentsOutput;
import com.rag.app.document.usecases.models.ProcessDocumentInput;
import com.rag.app.document.usecases.models.ProcessDocumentOutput;
import com.rag.app.document.usecases.models.UploadDocumentInput;
import com.rag.app.document.usecases.models.UploadDocumentOutput;

import java.util.List;
import java.util.Optional;

public interface DocumentManagementFacade {
    UploadDocumentOutput uploadDocument(UploadDocumentInput input);

    ProcessDocumentOutput processDocument(ProcessDocumentInput input);

    GetUserDocumentsOutput getUserDocuments(GetUserDocumentsInput input);

    GetAdminProgressOutput getAdminProgress(GetAdminProgressInput input);

    Optional<Document> findDocumentById(String documentId);

    List<Document> findDocumentsByUser(String userId);
}
