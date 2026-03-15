package com.rag.app.integration;

import com.rag.app.chat.usecases.models.QueryDocumentsOutput;
import com.rag.app.document.domain.valueobjects.DocumentStatus;
import com.rag.app.document.domain.valueobjects.FileType;
import com.rag.app.document.usecases.models.UploadDocumentInput;
import com.rag.app.document.usecases.models.UploadDocumentOutput;
import com.rag.app.integration.api.controllers.ChatController;
import com.rag.app.integration.api.controllers.DocumentController;
import com.rag.app.integration.api.controllers.UserController;
import com.rag.app.integration.events.EventBus;
import com.rag.app.integration.orchestration.ApplicationOrchestrator;
import com.rag.app.integration.orchestration.ModuleCoordinator;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ControllerIntegrationTest {
    @Test
    void shouldReturnApiResponsesFromControllers() {
        var docs = new ApplicationOrchestratorTest.StubDocumentManagementFacade();
        var chat = new ApplicationOrchestratorTest.StubChatSystemFacade();
        var users = new ApplicationOrchestratorTest.StubUserManagementFacade();
        ModuleCoordinator coordinator = new ModuleCoordinator();
        coordinator.register("document-management", "1.0.0", () -> true);
        coordinator.register("chat-system", "1.0.0", () -> true);
        coordinator.register("user-management", "1.0.0", () -> true);
        ApplicationOrchestrator orchestrator = new ApplicationOrchestrator(docs, chat, users, new EventBus(), coordinator);

        DocumentController documentController = new DocumentController(orchestrator, docs);
        ChatController chatController = new ChatController(orchestrator);
        UserController userController = new UserController(orchestrator);

        var uploadResponse = documentController.uploadDocument(new UploadDocumentInput("guide.md", 12L, FileType.MARKDOWN, "hello".getBytes(), users.user.userId().toString()));
        var chatResponse = chatController.submitQuery(users.user.userId().toString(), "question");
        var authResponse = userController.authenticate("admin", "password");

        assertTrue(uploadResponse.success());
        assertEquals(DocumentStatus.UPLOADED, uploadResponse.data().status());
        assertTrue(chatResponse.success());
        assertEquals("Integrated answer", chatResponse.data().answer());
        assertTrue(authResponse.success());
        assertTrue(authResponse.data().authenticated());
    }
}
