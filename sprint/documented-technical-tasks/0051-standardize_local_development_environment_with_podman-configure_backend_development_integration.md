# Configure Backend Development Integration

## Related User Story

User Story: standardize_local_development_environment_with_podman

## Objective

Configure the backend application to integrate seamlessly with the Podman-based supporting services while running natively in development mode, ensuring proper connectivity and configuration.

## Scope

- Update backend configuration for development services integration
- Configure database connections for containerized PostgreSQL
- Configure Weaviate vector database integration
- Configure Keycloak authentication integration
- Create development-specific application profiles
- Set up hot reload and development debugging

## Out of Scope

- Backend containerization (runs natively)
- Production configuration
- Advanced debugging tools setup
- Performance profiling configuration

## Clean Architecture Placement

infrastructure, interface adapters

## Execution Dependencies

- 0047-standardize_local_development_environment_with_podman-create_development_services_compose.md
- 0048-standardize_local_development_environment_with_podman-configure_keycloak_development_realm.md
- 0049-standardize_local_development_environment_with_podman-configure_development_database_setup.md
- 0050-standardize_local_development_environment_with_podman-configure_vector_database_development_setup.md

## Implementation Details

Update backend application configuration (backend/src/main/resources/application-dev.properties):
```properties
# Development Profile Configuration
%dev.quarkus.profile=dev

# Server Configuration
%dev.quarkus.http.port=8081
%dev.quarkus.http.host=0.0.0.0
%dev.quarkus.http.cors=true
%dev.quarkus.http.cors.origins=http://localhost:3000
%dev.quarkus.http.cors.headers=accept,authorization,content-type,x-requested-with
%dev.quarkus.http.cors.methods=GET,POST,PUT,DELETE,OPTIONS

# Database Configuration (Containerized PostgreSQL)
%dev.quarkus.datasource.db-kind=postgresql
%dev.quarkus.datasource.username=rag_dev_user
%dev.quarkus.datasource.password=rag_dev_password
%dev.quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/rag_app_dev
%dev.quarkus.datasource.jdbc.max-size=20
%dev.quarkus.datasource.jdbc.min-size=5

# Database Migration (Development)
%dev.quarkus.flyway.migrate-at-start=false
%dev.quarkus.hibernate-orm.database.generation=none
%dev.quarkus.hibernate-orm.sql-load-script=no-file

# Keycloak Configuration (Containerized)
%dev.quarkus.oidc.auth-server-url=http://localhost:8180/realms/rag-app-dev
%dev.quarkus.oidc.client-id=rag-app-backend
%dev.quarkus.oidc.credentials.secret=backend-dev-secret
%dev.quarkus.oidc.tls.verification=none
%dev.quarkus.oidc.discovery-enabled=true

# Security Configuration
%dev.quarkus.http.auth.permission.authenticated.paths=/api/*
%dev.quarkus.http.auth.permission.authenticated.policy=authenticated
%dev.quarkus.http.auth.permission.admin.paths=/api/admin/*
%dev.quarkus.http.auth.permission.admin.policy=role-admin
%dev.quarkus.http.auth.permission.health.paths=/q/health
%dev.quarkus.http.auth.permission.health.policy=permit

# Vector Database Configuration (Containerized Weaviate)
%dev.app.vectorstore.provider=weaviate
%dev.app.vectorstore.url=http://localhost:8080
%dev.app.vectorstore.api-key=
%dev.app.vectorstore.timeout=30000
%dev.app.vectorstore.batch-size=100

# LLM Configuration (Containerized Ollama)
%dev.app.llm.provider=ollama
%dev.app.llm.url=http://localhost:11434
%dev.app.llm.model=llama2:7b-chat
%dev.app.llm.timeout=20000
%dev.app.llm.max-tokens=2048
%dev.app.llm.temperature=0.1

# Redis Configuration (Containerized)
%dev.quarkus.redis.hosts=redis://localhost:6379
%dev.quarkus.redis.timeout=10s

# File Storage Configuration
%dev.app.storage.documents.path=./storage/documents
%dev.app.storage.max-file-size=41943040
%dev.app.storage.allowed-types=PDF,MARKDOWN,PLAIN_TEXT

# Logging Configuration
%dev.quarkus.log.level=INFO
%dev.quarkus.log.category."com.rag.app".level=DEBUG
%dev.quarkus.log.category."org.hibernate.SQL".level=DEBUG
%dev.quarkus.log.category."org.hibernate.type.descriptor.sql.BasicBinder".level=TRACE
%dev.quarkus.log.console.enable=true
%dev.quarkus.log.console.format=%d{HH:mm:ss} %-5p [%c{2.}] (%t) %s%e%n

# Development Features
%dev.quarkus.live-reload.instrumentation=true
%dev.quarkus.dev.instrumentation=true
%dev.quarkus.swagger-ui.always-include=true
%dev.quarkus.smallrye-openapi.info-title=RAG Application API (Development)
%dev.quarkus.smallrye-openapi.info-version=1.0.0-dev

# Health Checks
%dev.quarkus.smallrye-health.root-path=/q/health
%dev.quarkus.smallrye-health.liveness-path=/q/health/live
%dev.quarkus.smallrye-health.readiness-path=/q/health/ready

# Metrics (Development)
%dev.quarkus.micrometer.enabled=true
%dev.quarkus.micrometer.export.prometheus.enabled=true
%dev.quarkus.micrometer.export.prometheus.path=/q/metrics
```

Create development startup script (backend/start-dev.sh):
```bash
#!/bin/bash
set -e

echo "=== Starting RAG Backend in Development Mode ==="

# Check if supporting services are running
echo "Checking supporting services..."

# Check PostgreSQL
if ! pg_isready -h localhost -p 5432 -U rag_dev_user > /dev/null 2>&1; then
    echo "ERROR: PostgreSQL is not running"
    echo "Please start development services first: ./start-dev-services.sh"
    exit 1
fi
echo "✓ PostgreSQL is running"

# Check Weaviate
if ! curl -f http://localhost:8080/v1/meta > /dev/null 2>&1; then
    echo "ERROR: Weaviate is not running"
    echo "Please start development services first: ./start-dev-services.sh"
    exit 1
fi
echo "✓ Weaviate is running"

# Check Keycloak
if ! curl -f http://localhost:8180/health/ready > /dev/null 2>&1; then
    echo "ERROR: Keycloak is not running"
    echo "Please start development services first: ./start-dev-services.sh"
    exit 1
fi
echo "✓ Keycloak is running"

# Create storage directory if it doesn't exist
mkdir -p ./storage/documents
echo "✓ Storage directory ready"

# Set development environment
export QUARKUS_PROFILE=dev
export JAVA_OPTS="-Xmx2g -XX:+UseG1GC -Dquarkus.profile=dev"

echo ""
echo "Starting Quarkus in development mode..."
echo "Backend will be available at: http://localhost:8081"
echo "API Documentation: http://localhost:8081/q/swagger-ui"
echo "Health Check: http://localhost:8081/q/health"
echo ""
echo "Press Ctrl+C to stop the backend"
echo ""

# Start Quarkus in development mode
./gradlew quarkusDev
```

Create backend health check configuration (backend/src/main/java/com/rag/app/health/DevelopmentHealthCheck.java):
```java
package com.rag.app.health;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;

import io.agroal.api.AgroalDataSource;

@Readiness
@ApplicationScoped
public class DevelopmentHealthCheck implements HealthCheck {

    @Inject
    AgroalDataSource dataSource;

    @ConfigProperty(name = "app.vectorstore.url")
    String weaviateUrl;

    @ConfigProperty(name = "quarkus.oidc.auth-server-url")
    String keycloakUrl;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Override
    public HealthCheckResponse call() {
        var builder = HealthCheckResponse.named("development-services");

        try {
            boolean allHealthy = checkDatabase() && checkWeaviate() && checkKeycloak();
            
            if (allHealthy) {
                builder.up();
            } else {
                builder.down();
            }
            
            builder.withData("database", checkDatabase())
                   .withData("weaviate", checkWeaviate())
                   .withData("keycloak", checkKeycloak());
                   
        } catch (Exception e) {
            builder.down().withData("error", e.getMessage());
        }

        return builder.build();
    }

    private boolean checkDatabase() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(5);
        } catch (SQLException e) {
            return false;
        }
    }

    private boolean checkWeaviate() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(weaviateUrl + "/v1/meta"))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, 
                    HttpResponse.BodyHandlers.ofString());
            
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean checkKeycloak() {
        try {
            // Extract base URL from auth-server-url
            String baseUrl = keycloakUrl.substring(0, keycloakUrl.indexOf("/realms"));
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/health/ready"))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, 
                    HttpResponse.BodyHandlers.ofString());
            
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }
}
```

Create development configuration class (backend/src/main/java/com/rag/app/config/DevelopmentConfiguration.java):
```java
package com.rag.app.config;

import io.quarkus.runtime.StartupEvent;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@ApplicationScoped
public class DevelopmentConfiguration {

    private static final Logger LOG = Logger.getLogger(DevelopmentConfiguration.class);

    @ConfigProperty(name = "app.storage.documents.path")
    String documentsPath;

    @ConfigProperty(name = "quarkus.profile")
    String profile;

    void onStart(@Observes StartupEvent ev) {
        if ("dev".equals(profile)) {
            LOG.info("=== RAG Application Development Mode ===");
            
            // Create storage directories
            createStorageDirectories();
            
            // Log configuration
            logDevelopmentConfiguration();
            
            LOG.info("=== Development setup complete ===");
        }
    }

    private void createStorageDirectories() {
        try {
            Path documentsDir = Paths.get(documentsPath);
            if (!Files.exists(documentsDir)) {
                Files.createDirectories(documentsDir);
                LOG.info("Created documents storage directory: " + documentsDir);
            }
        } catch (IOException e) {
            LOG.error("Failed to create storage directories", e);
        }
    }

    private void logDevelopmentConfiguration() {
        LOG.info("Development Configuration:");
        LOG.info("  Profile: " + profile);
        LOG.info("  Documents Storage: " + documentsPath);
        LOG.info("  Database: jdbc:postgresql://localhost:5432/rag_app_dev");
        LOG.info("  Weaviate: http://localhost:8080");
        LOG.info("  Keycloak: http://localhost:8180");
        LOG.info("  Backend API: http://localhost:8081");
        LOG.info("  Swagger UI: http://localhost:8081/q/swagger-ui");
    }
}
```

Create development testing script (backend/test-dev-integration.sh):
```bash
#!/bin/bash
set -e

BACKEND_URL="http://localhost:8081"

echo "=== Testing Backend Development Integration ==="

# Wait for backend to be ready
echo "Waiting for backend to be ready..."
until curl -f "${BACKEND_URL}/q/health/ready" > /dev/null 2>&1; do
    echo -n "."
    sleep 2
done
echo " ✓"

# Test health endpoints
echo ""
echo "Testing health endpoints..."

echo -n "Health check: "
HEALTH_STATUS=$(curl -s "${BACKEND_URL}/q/health" | jq -r '.status')
if [ "$HEALTH_STATUS" = "UP" ]; then
    echo "✓ UP"
else
    echo "✗ $HEALTH_STATUS"
fi

echo -n "Readiness check: "
READY_STATUS=$(curl -s "${BACKEND_URL}/q/health/ready" | jq -r '.status')
if [ "$READY_STATUS" = "UP" ]; then
    echo "✓ UP"
else
    echo "✗ $READY_STATUS"
fi

echo -n "Liveness check: "
LIVE_STATUS=$(curl -s "${BACKEND_URL}/q/health/live" | jq -r '.status')
if [ "$LIVE_STATUS" = "UP" ]; then
    echo "✓ UP"
else
    echo "✗ $LIVE_STATUS"
fi

# Test development services integration
echo ""
echo "Testing development services integration..."

SERVICES_HEALTH=$(curl -s "${BACKEND_URL}/q/health" | jq -r '.checks[] | select(.name == "development-services")')
if [ -n "$SERVICES_HEALTH" ]; then
    DB_STATUS=$(echo "$SERVICES_HEALTH" | jq -r '.data.database')
    WEAVIATE_STATUS=$(echo "$SERVICES_HEALTH" | jq -r '.data.weaviate')
    KEYCLOAK_STATUS=$(echo "$SERVICES_HEALTH" | jq -r '.data.keycloak')
    
    echo "Database: $([ "$DB_STATUS" = "true" ] && echo "✓" || echo "✗")"
    echo "Weaviate: $([ "$WEAVIATE_STATUS" = "true" ] && echo "✓" || echo "✗")"
    echo "Keycloak: $([ "$KEYCLOAK_STATUS" = "true" ] && echo "✓" || echo "✗")"
else
    echo "✗ Development services health check not found"
fi

# Test API endpoints
echo ""
echo "Testing API endpoints..."

echo -n "Swagger UI: "
if curl -f "${BACKEND_URL}/q/swagger-ui" > /dev/null 2>&1; then
    echo "✓ Available"
else
    echo "✗ Not accessible"
fi

echo -n "OpenAPI spec: "
if curl -f "${BACKEND_URL}/q/openapi" > /dev/null 2>&1; then
    echo "✓ Available"
else
    echo "✗ Not accessible"
fi

# Test CORS configuration
echo ""
echo "Testing CORS configuration..."
CORS_RESPONSE=$(curl -s -H "Origin: http://localhost:3000" \
    -H "Access-Control-Request-Method: GET" \
    -H "Access-Control-Request-Headers: Content-Type" \
    -X OPTIONS "${BACKEND_URL}/api/health")

if echo "$CORS_RESPONSE" | grep -q "Access-Control-Allow-Origin"; then
    echo "✓ CORS configured correctly"
else
    echo "✗ CORS not configured"
fi

echo ""
echo "=== Backend Development Integration Test Complete ==="
```

Update Gradle configuration for development (backend/build.gradle):
```gradle
// Add development-specific configuration
quarkus {
    buildNative {
        enabled = false
    }
}

// Development task
task dev {
    group = 'application'
    description = 'Runs the application in development mode with supporting services'
    dependsOn 'quarkusDev'
    
    doFirst {
        println "Starting RAG Backend in development mode..."
        println "Make sure supporting services are running: ./start-dev-services.sh"
    }
}

// Development with debug
task devDebug {
    group = 'application'
    description = 'Runs the application in development mode with debugging enabled'
    
    doFirst {
        System.setProperty('quarkus.debug.enabled', 'true')
        System.setProperty('quarkus.debug.port', '5005')
    }
    
    finalizedBy 'quarkusDev'
}

// Test development integration
task testDevIntegration(type: Exec) {
    group = 'verification'
    description = 'Tests integration with development services'
    commandLine './test-dev-integration.sh'
}
```

## Files / Modules Impacted

- backend/src/main/resources/application-dev.properties
- backend/start-dev.sh
- backend/src/main/java/com/rag/app/health/DevelopmentHealthCheck.java
- backend/src/main/java/com/rag/app/config/DevelopmentConfiguration.java
- backend/test-dev-integration.sh
- backend/build.gradle (extend with dev tasks)

## Acceptance Criteria

Given the backend is configured for development
When ./start-dev.sh is executed
Then the backend should start and connect to all supporting services

Given supporting services are running
When the backend health check is performed
Then all services should report as healthy

Given the backend is running in development mode
When API requests are made from the frontend
Then CORS should be properly configured

Given development debugging is needed
When the backend is started with debug mode
Then it should be accessible for debugging on port 5005

## Testing Requirements

- Test backend startup with supporting services
- Test health checks for all integrated services
- Test CORS configuration for frontend integration
- Test API endpoints accessibility
- Test development hot reload functionality

## Dependencies / Preconditions

- Supporting services must be running (PostgreSQL, Weaviate, Keycloak)
- Java 25 must be available
- Gradle must be configured
- Network connectivity between services must be established