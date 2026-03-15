package com.rag.app.integration.events;

import com.rag.app.integration.events.events.DocumentUploadedEvent;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class EventBusIntegrationTest {
    @Test
    void shouldDeliverPublishedEventsToAllRegisteredHandlers() {
        EventBus eventBus = new EventBus();
        AtomicInteger invocations = new AtomicInteger();

        eventBus.register(DocumentUploadedEvent.class, event -> invocations.incrementAndGet());
        eventBus.register(DocumentUploadedEvent.class, event -> invocations.incrementAndGet());
        eventBus.publish(new DocumentUploadedEvent("doc-1", "guide.md", "user-1"));

        Awaitility.await().atMost(Duration.ofSeconds(5)).untilAtomic(invocations, org.hamcrest.Matchers.equalTo(2));
        assertThat(invocations.get()).isEqualTo(2);
        eventBus.close();
    }
}
