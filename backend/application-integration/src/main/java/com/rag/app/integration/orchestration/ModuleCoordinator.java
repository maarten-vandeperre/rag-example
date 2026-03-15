package com.rag.app.integration.orchestration;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BooleanSupplier;

public final class ModuleCoordinator {
    private final Map<String, ModuleRegistration> modules = new LinkedHashMap<>();

    public void register(String moduleName, String moduleVersion, BooleanSupplier healthCheck) {
        modules.put(moduleName, new ModuleRegistration(moduleName, moduleVersion, healthCheck));
    }

    public Map<String, Boolean> healthSnapshot() {
        Map<String, Boolean> snapshot = new LinkedHashMap<>();
        for (ModuleRegistration module : modules.values()) {
            snapshot.put(module.moduleName(), module.healthCheck().getAsBoolean());
        }
        return snapshot;
    }

    public boolean allHealthy() {
        return healthSnapshot().values().stream().allMatch(Boolean::booleanValue);
    }

    private record ModuleRegistration(String moduleName, String moduleVersion, BooleanSupplier healthCheck) {
        private ModuleRegistration {
            Objects.requireNonNull(moduleName, "moduleName must not be null");
            Objects.requireNonNull(moduleVersion, "moduleVersion must not be null");
            Objects.requireNonNull(healthCheck, "healthCheck must not be null");
        }
    }
}
