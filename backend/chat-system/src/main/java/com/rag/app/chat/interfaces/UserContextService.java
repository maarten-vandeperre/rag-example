package com.rag.app.chat.interfaces;

import com.rag.app.chat.domain.valueobjects.UserRole;

public interface UserContextService {
    UserRole getUserRole(String userId);

    boolean isActiveUser(String userId);
}
