package com.rag.app.document.domain.services;

import com.rag.app.document.domain.entities.DocumentUser;
import com.rag.app.document.domain.valueobjects.DocumentMetadata;
import com.rag.app.document.domain.valueobjects.UserRole;

public final class DocumentDomainService {
    public void validateUploadRequest(DocumentUser user, long fileSize) {
        if (!user.active()) {
            throw new IllegalArgumentException("uploadedBy user must be active");
        }
        if (fileSize <= 0 || fileSize > DocumentMetadata.MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("fileSize must be positive and less than or equal to 41943040");
        }
    }

    public boolean canViewAllDocuments(DocumentUser user, boolean includeAllDocuments) {
        return user.role() == UserRole.ADMIN && includeAllDocuments;
    }

    public void ensureAdmin(DocumentUser user) {
        if (user.role() != UserRole.ADMIN) {
            throw new IllegalArgumentException("adminUserId user must be an admin");
        }
    }
}
