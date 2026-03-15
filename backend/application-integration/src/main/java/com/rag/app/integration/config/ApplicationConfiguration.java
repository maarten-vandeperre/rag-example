package com.rag.app.integration.config;

import com.rag.app.chat.interfaces.ChatSystemFacade;
import com.rag.app.document.interfaces.DocumentManagementFacade;
import com.rag.app.integration.events.EventBus;
import com.rag.app.integration.events.events.DocumentProcessedEvent;
import com.rag.app.integration.events.events.DocumentUploadedEvent;
import com.rag.app.integration.events.events.UserAuthenticatedEvent;
import com.rag.app.integration.orchestration.ApplicationOrchestrator;
import com.rag.app.user.interfaces.UserManagementFacade;

public final class ApplicationConfiguration {
    public ApplicationOrchestrator applicationOrchestrator(DocumentManagementFacade documentManagement,
                                                           ChatSystemFacade chatSystem,
                                                           UserManagementFacade userManagement) {
        EventBus eventBus = new EventBus();
        ApplicationOrchestrator orchestrator = new ApplicationOrchestrator(
            documentManagement,
            chatSystem,
            userManagement,
            eventBus,
            new ModuleConfiguration().moduleCoordinator()
        );
        eventBus.register(DocumentUploadedEvent.class, orchestrator::handleDocumentUploaded);
        eventBus.register(DocumentProcessedEvent.class, orchestrator::handleDocumentProcessed);
        eventBus.register(UserAuthenticatedEvent.class, orchestrator::handleUserAuthenticated);
        return orchestrator;
    }
}
