package com.rag.app.integration.config;

import com.rag.app.integration.orchestration.ModuleCoordinator;

public final class ModuleConfiguration {
    public ModuleCoordinator moduleCoordinator() {
        ModuleCoordinator coordinator = new ModuleCoordinator();
        coordinator.register("document-management", "1.0.0", () -> true);
        coordinator.register("chat-system", "1.0.0", () -> true);
        coordinator.register("user-management", "1.0.0", () -> true);
        return coordinator;
    }
}
