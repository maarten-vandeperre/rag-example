package com.rag.app.user.interfaces;

import com.rag.app.user.domain.valueobjects.UserId;

import java.util.List;

public interface ChatAuthorizer {
    boolean canQueryDocuments(UserId userId, List<String> documentIds);

    boolean canViewChatHistory(UserId userId, String chatOwnerId);
}
