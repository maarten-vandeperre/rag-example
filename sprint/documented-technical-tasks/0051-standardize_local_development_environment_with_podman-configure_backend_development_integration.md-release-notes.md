## Summary
Configured the backend for native development against the Podman-based supporting services with a richer dev profile, development startup scripts, and a readiness health check for PostgreSQL, Weaviate, and Keycloak.

## Changes
Updated `backend/src/main/resources/application-dev.properties` with development HTTP, datasource, OIDC, vector store, Redis, LLM, logging, health, and OpenAPI settings.
Added `backend/src/main/java/com/rag/app/health/DevelopmentHealthCheck.java` and `backend/src/main/java/com/rag/app/config/DevelopmentConfiguration.java` for development health visibility and startup initialization.
Added `backend/start-dev.sh` and `backend/test-dev-integration.sh` to bootstrap and validate native backend development against local services.
Updated `backend/build.gradle` to include development integration dependencies and dev-focused Gradle tasks.

## Impact
The backend can now run in a more complete local-development mode with explicit service integration settings and operational checks for the external services it depends on.

## Verification
Executed `chmod +x backend/start-dev.sh backend/test-dev-integration.sh`.
Executed `bash -n backend/start-dev.sh backend/test-dev-integration.sh`.
Executed `./gradlew --no-daemon :backend:test`.
Executed `./gradlew --no-daemon healthCheck :backend:test`.

## Follow-ups
Wire the new development integration scripts into the remaining frontend and documentation tasks, and consider adding a Quarkus integration test that asserts the new health check payload in dev mode.
