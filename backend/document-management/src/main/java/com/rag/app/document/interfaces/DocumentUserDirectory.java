package com.rag.app.document.interfaces;

import com.rag.app.document.domain.entities.DocumentUser;

import java.util.Optional;

public interface DocumentUserDirectory {
    Optional<DocumentUser> findById(String userId);
}
