## Summary
Added a minimal Quarkus backend application, multi-stage Dockerfile, and runtime configuration so the compose stack can build and run the backend service.

## Changes
- `backend/Dockerfile`
- `backend/.dockerignore`
- `backend/pom.xml`
- `backend/maven-settings.xml`
- `backend/src/main/java/com/rag/app/api/BackendStatusResource.java`
- `backend/src/main/resources/application.properties`
- `backend/src/test/java/com/rag/app/api/BackendStatusResourceTest.java`
- `docker-compose.yml`

## Impact
The repository now contains a buildable backend service image with health endpoints, document storage volume wiring, and configuration hooks for PostgreSQL, Weaviate, and Ollama.

## Verification
- `docker compose --env-file .env.development config`
- `mvn -s maven-settings.xml -Dmaven.repo.local=/tmp/rag-example-backend-m2 test`
- `mvn -s maven-settings.xml -Dmaven.repo.local=/tmp/rag-example-backend-m2 package -DskipTests`
- `podman-compose --env-file .env.development build backend`
- `podman-compose --env-file .env.development up -d postgres weaviate weaviate-init ollama ollama-init backend`
- `curlimages/curl` requests against `http://backend:8080/q/health` and `http://backend:8080/api/health`
- `podman exec rag-example_backend_1 sh -lc 'test -d /app/storage/documents && ls -ld /app/storage/documents'`
- `podman-compose --env-file .env.development down`

## Follow-ups
Connect the backend to real persistence and vector-store adapters once the domain and use-case layers are added in later tasks.
