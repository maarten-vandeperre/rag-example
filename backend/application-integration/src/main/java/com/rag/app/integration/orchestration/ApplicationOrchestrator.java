package com.rag.app.integration.orchestration;

import com.rag.app.chat.interfaces.ChatSystemFacade;
import com.rag.app.chat.usecases.models.QueryDocumentsInput;
import com.rag.app.chat.usecases.models.QueryDocumentsOutput;
import com.rag.app.document.domain.valueobjects.DocumentStatus;
import com.rag.app.document.interfaces.DocumentManagementFacade;
import com.rag.app.document.usecases.models.DocumentSummary;
import com.rag.app.document.usecases.models.GetUserDocumentsInput;
import com.rag.app.document.usecases.models.ProcessDocumentInput;
import com.rag.app.integration.events.EventBus;
import com.rag.app.integration.events.events.ChatQuerySubmittedEvent;
import com.rag.app.integration.events.events.DocumentProcessedEvent;
import com.rag.app.integration.events.events.DocumentUploadedEvent;
import com.rag.app.integration.events.events.UserAuthenticatedEvent;
import com.rag.app.user.domain.valueobjects.UserId;
import com.rag.app.user.interfaces.UserManagementFacade;
import com.rag.app.user.usecases.models.AuthenticationRequest;
import com.rag.app.user.usecases.models.AuthenticationResult;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ApplicationOrchestrator {
    private final DocumentManagementFacade documentManagement;
    private final ChatSystemFacade chatSystem;
    private final UserManagementFacade userManagement;
    private final EventBus eventBus;
    private final ModuleCoordinator moduleCoordinator;

    public ApplicationOrchestrator(DocumentManagementFacade documentManagement,
                                   ChatSystemFacade chatSystem,
                                   UserManagementFacade userManagement,
                                   EventBus eventBus,
                                   ModuleCoordinator moduleCoordinator) {
        this.documentManagement = documentManagement;
        this.chatSystem = chatSystem;
        this.userManagement = userManagement;
        this.eventBus = eventBus;
        this.moduleCoordinator = moduleCoordinator;
    }

    public AuthenticationResult authenticateUser(AuthenticationRequest request) {
        AuthenticationResult result = userManagement.authenticateUser(request);
        if (result.authenticated()) {
            eventBus.publish(new UserAuthenticatedEvent(result.user().userId().toString(), result.user().role()));
        }
        return result;
    }

    public QueryDocumentsOutput processQuery(String userId, String question) {
        UserId requestedUserId = new UserId(UUID.fromString(userId));
        if (!userManagement.isAuthorized(requestedUserId, "query_documents", "read")) {
            throw new IllegalArgumentException("User not authorized to query documents");
        }

        List<String> accessibleDocumentIds = getAccessibleDocuments(userId);
        QueryDocumentsOutput result = chatSystem.queryDocuments(new QueryDocumentsInput(userId, question));
        eventBus.publish(new ChatQuerySubmittedEvent(userId, question, UUID.randomUUID().toString()));
        if (result.success() && !accessibleDocumentIds.isEmpty()) {
            return result;
        }
        return result;
    }

    public void handleDocumentUploaded(DocumentUploadedEvent event) {
        var processResult = documentManagement.processDocument(new ProcessDocumentInput(UUID.fromString(event.documentId())));
        String extractedContent = processResult.finalStatus() == DocumentStatus.READY
            ? "processed-content-" + event.documentId()
            : null;
        eventBus.publish(new DocumentProcessedEvent(event.documentId(), processResult.finalStatus(), extractedContent));
    }

    public void handleDocumentProcessed(DocumentProcessedEvent event) {
        if (event.status() == DocumentStatus.READY && event.extractedContent() != null) {
            chatSystem.storeDocumentVectors(event.documentId(), event.extractedContent());
        }
    }

    public void handleUserAuthenticated(UserAuthenticatedEvent event) {
        if (!userManagement.isActiveUser(new UserId(UUID.fromString(event.userId())))) {
            throw new IllegalArgumentException("Authenticated user must remain active");
        }
    }

    public void publishEvent(com.rag.app.shared.domain.events.DomainEvent event) {
        eventBus.publish(event);
    }

    public Map<String, Boolean> moduleHealthSnapshot() {
        return moduleCoordinator.healthSnapshot();
    }

    private List<String> getAccessibleDocuments(String userId) {
        boolean includeAll = userManagement.getUserRole(new UserId(UUID.fromString(userId))).name().equals("ADMIN");
        return documentManagement.getUserDocuments(new GetUserDocumentsInput(userId, includeAll)).documents().stream()
            .map(DocumentSummary::documentId)
            .map(UUID::toString)
            .toList();
    }
}
