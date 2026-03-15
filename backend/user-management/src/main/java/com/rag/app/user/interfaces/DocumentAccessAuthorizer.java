package com.rag.app.user.interfaces;

import com.rag.app.user.domain.valueobjects.UserId;

import java.util.List;

public interface DocumentAccessAuthorizer {
    boolean canAccessDocument(UserId userId, String documentId);

    boolean canAccessAllDocuments(UserId userId);

    List<String> getAccessibleDocumentIds(UserId userId);
}
