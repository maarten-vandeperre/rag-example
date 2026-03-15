package com.rag.app.health;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javax.sql.DataSource;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;

@Readiness
@ApplicationScoped
public class DevelopmentHealthCheck implements HealthCheck {

    @Inject
    DataSource dataSource;

    @ConfigProperty(name = "app.vectorstore.url", defaultValue = "http://localhost:8080")
    String weaviateUrl;

    @ConfigProperty(name = "quarkus.oidc.auth-server-url", defaultValue = "http://localhost:8180/realms/rag-app-dev")
    String keycloakUrl;

    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build();

    @Override
    public HealthCheckResponse call() {
        boolean databaseHealthy = checkDatabase();
        boolean weaviateHealthy = checkHttpEndpoint(URI.create(weaviateUrl + "/v1/meta"));
        boolean keycloakHealthy = checkKeycloak();

        HealthCheckResponseBuilder builder = HealthCheckResponse.named("development-services")
            .withData("database", databaseHealthy)
            .withData("weaviate", weaviateHealthy)
            .withData("keycloak", keycloakHealthy);

        if (databaseHealthy && weaviateHealthy && keycloakHealthy) {
            builder.up();
        } else {
            builder.down();
        }

        return builder.build();
    }

    private boolean checkDatabase() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(5);
        } catch (SQLException exception) {
            return false;
        }
    }

    private boolean checkKeycloak() {
        int realmIndex = keycloakUrl.indexOf("/realms");
        String baseUrl = realmIndex >= 0 ? keycloakUrl.substring(0, realmIndex) : keycloakUrl;
        return checkHttpEndpoint(URI.create(baseUrl + "/health/ready"));
    }

    private boolean checkHttpEndpoint(URI uri) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception exception) {
            return false;
        }
    }
}
