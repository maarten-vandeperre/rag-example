package com.rag.app.integration.system;

import com.rag.app.document.domain.valueobjects.FileType;
import com.rag.app.document.usecases.models.UploadDocumentInput;
import com.rag.app.integration.api.controllers.ChatController;
import com.rag.app.integration.api.controllers.DocumentController;
import com.rag.app.integration.api.controllers.UserController;
import com.rag.app.integration.events.EventBus;
import com.rag.app.integration.support.IntegrationTestFixtures;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BackwardCompatibilityTest {
    @Test
    void shouldPreserveExpectedApiResponseShapes() {
        EventBus eventBus = new EventBus();
        var orchestrator = IntegrationTestFixtures.orchestrator(eventBus);
        var docs = (IntegrationTestFixtures.StubDocumentManagementFacade) readField(orchestrator, "documentManagement");
        var users = (IntegrationTestFixtures.StubUserManagementFacade) readField(orchestrator, "userManagement");

        var documentController = new DocumentController(orchestrator, docs);
        var chatController = new ChatController(orchestrator);
        var userController = new UserController(orchestrator);

        var upload = documentController.uploadDocument(new UploadDocumentInput("guide.md", 12L, FileType.MARKDOWN, "hello".getBytes(), users.user.userId().toString()));
        var documents = documentController.getUserDocuments(users.user.userId().toString(), true);
        var chat = chatController.submitQuery(users.user.userId().toString(), "question");
        var auth = userController.authenticate("admin", "password");

        assertThat(upload.data().documentId()).isNotNull();
        assertThat(documents.data().documents()).hasSize(1);
        assertThat(documents.data().documents().get(0).fileName()).isEqualTo("guide.md");
        assertThat(chat.data().success()).isTrue();
        assertThat(auth.data().authenticated()).isTrue();
        eventBus.close();
    }

    private Object readField(Object target, String fieldName) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(target);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }
}
