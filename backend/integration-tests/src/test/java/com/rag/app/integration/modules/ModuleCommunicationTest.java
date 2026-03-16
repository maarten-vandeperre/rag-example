package com.rag.app.integration.modules;

import com.rag.app.integration.events.EventBus;
import com.rag.app.integration.events.events.ChatQuerySubmittedEvent;
import com.rag.app.integration.events.events.DocumentProcessedEvent;
import com.rag.app.integration.events.events.DocumentUploadedEvent;
import com.rag.app.integration.support.IntegrationTestFixtures;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class ModuleCommunicationTest {
    @Test
    void shouldCommunicateAcrossModulesThroughEvents() {
        EventBus eventBus = new EventBus();
        var orchestrator = IntegrationTestFixtures.orchestrator(eventBus);
        var docs = (IntegrationTestFixtures.StubDocumentManagementFacade) readField(orchestrator, "documentManagement");
        AtomicBoolean processed = new AtomicBoolean(false);
        AtomicBoolean queried = new AtomicBoolean(false);
        AtomicReference<DocumentProcessedEvent> processedEvent = new AtomicReference<>();

        eventBus.register(DocumentUploadedEvent.class, orchestrator::handleDocumentUploaded);
        eventBus.register(DocumentProcessedEvent.class, event -> {
            orchestrator.handleDocumentProcessed(event);
            processedEvent.set(event);
            processed.set(true);
        });
        eventBus.register(ChatQuerySubmittedEvent.class, event -> queried.set(true));

        orchestrator.publishEvent(new DocumentUploadedEvent(docs.documentId.toString(), "guide.md", "22222222-2222-2222-2222-222222222222"));
        orchestrator.processQuery("22222222-2222-2222-2222-222222222222", "Where is the guide?");

        Awaitility.await().atMost(Duration.ofSeconds(5)).untilTrue(processed);
        Awaitility.await().atMost(Duration.ofSeconds(5)).untilTrue(queried);
        assertThat(processedEvent.get().documentId()).isEqualTo(docs.documentId.toString());
        assertThat(processedEvent.get().extractedTextLength()).isEqualTo(42);
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
