package com.rag.app.integration.system;

import com.rag.app.integration.events.EventBus;
import com.rag.app.integration.support.IntegrationTestFixtures;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SystemHealthTest {
    @Test
    void shouldReportAllModulesHealthy() {
        EventBus eventBus = new EventBus();
        var orchestrator = IntegrationTestFixtures.orchestrator(eventBus);

        assertThat(orchestrator.moduleHealthSnapshot())
            .containsEntry("document-management", true)
            .containsEntry("chat-system", true)
            .containsEntry("user-management", true);
        eventBus.close();
    }
}
