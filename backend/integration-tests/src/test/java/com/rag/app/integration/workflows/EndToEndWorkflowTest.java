package com.rag.app.integration.workflows;

import com.rag.app.integration.events.EventBus;
import com.rag.app.integration.events.events.DocumentProcessedEvent;
import com.rag.app.integration.events.events.DocumentUploadedEvent;
import com.rag.app.integration.support.IntegrationTestFixtures;
import com.rag.app.user.usecases.models.AuthenticationRequest;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class EndToEndWorkflowTest {
    @Test
    void shouldCompleteAuthenticationUploadAndQueryWorkflow() {
        EventBus eventBus = new EventBus();
        var orchestrator = IntegrationTestFixtures.orchestrator(eventBus);
        var docs = (IntegrationTestFixtures.StubDocumentManagementFacade) readField(orchestrator, "documentManagement");
        AtomicBoolean processed = new AtomicBoolean(false);

        eventBus.register(DocumentUploadedEvent.class, orchestrator::handleDocumentUploaded);
        eventBus.register(DocumentProcessedEvent.class, event -> processed.set(true));

        var authentication = orchestrator.authenticateUser(new AuthenticationRequest("admin", "password"));
        orchestrator.publishEvent(new DocumentUploadedEvent(docs.documentId.toString(), "guide.md", authentication.user().userId().toString()));
        var query = orchestrator.processQuery(authentication.user().userId().toString(), "Summarize the content");

        Awaitility.await().atMost(Duration.ofSeconds(5)).untilTrue(processed);
        assertThat(authentication.authenticated()).isTrue();
        assertThat(query.success()).isTrue();
        assertThat(query.documentReferences()).isNotEmpty();
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
