package com.rag.app.chat.interfaces;

import com.rag.app.chat.domain.valueobjects.UserRole;
import com.rag.app.chat.usecases.models.DocumentSummary;

import java.util.List;

public interface DocumentAccessService {
    List<DocumentSummary> getAccessibleDocuments(String userId, UserRole role);

    boolean isDocumentAccessible(String documentId, String userId, UserRole role);
}
