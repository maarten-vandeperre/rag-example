package com.rag.app.config;

import io.quarkus.runtime.StartupEvent;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@ApplicationScoped
public class DevelopmentConfiguration {
    private static final Logger LOG = Logger.getLogger(DevelopmentConfiguration.class);

    @ConfigProperty(name = "app.storage.documents.path")
    String documentsPath;

    @ConfigProperty(name = "quarkus.profile", defaultValue = "prod")
    String profile;

    @ConfigProperty(name = "app.llm.provider", defaultValue = "heuristic")
    String llmProvider;

    @ConfigProperty(name = "app.llm.url", defaultValue = "http://localhost:11434")
    String llmUrl;

    @ConfigProperty(name = "app.llm.model", defaultValue = "tinyllama")
    String llmModel;

    void onStart(@Observes StartupEvent ignoredEvent) {
        if (!"dev".equals(profile)) {
            return;
        }

        LOG.info("=== RAG Application Development Mode ===");
        createStorageDirectories();
        logDevelopmentConfiguration();
        LOG.info("=== Development setup complete ===");
    }

    private void createStorageDirectories() {
        try {
            Path documentsDir = Path.of(documentsPath);
            if (Files.notExists(documentsDir)) {
                Files.createDirectories(documentsDir);
                LOG.infof("Created documents storage directory: %s", documentsDir);
            }
        } catch (IOException exception) {
            LOG.error("Failed to create storage directories", exception);
        }
    }

    private void logDevelopmentConfiguration() {
        LOG.info("Development Configuration:");
        LOG.infof("  Profile: %s", profile);
        LOG.infof("  Documents Storage: %s", documentsPath);
        LOG.info("  Database: jdbc:postgresql://localhost:5432/rag_app_dev");
        LOG.info("  Weaviate: http://localhost:8080");
        LOG.info("  Keycloak: http://localhost:8180");
        LOG.infof("  LLM Provider: %s (%s, model=%s)", llmProvider, llmUrl, llmModel);
        LOG.info("  Backend API: http://localhost:8081");
        LOG.info("  Swagger UI: http://localhost:8081/q/swagger-ui");
    }
}
