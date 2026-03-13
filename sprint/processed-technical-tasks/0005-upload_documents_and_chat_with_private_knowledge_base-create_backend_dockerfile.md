# Create Backend Dockerfile

## Related User Story

User Story: upload_documents_and_chat_with_private_knowledge_base

## Objective

Create Dockerfile for the Quarkus backend application with proper Java runtime, dependency management, and application configuration.

## Scope

- Create multi-stage Dockerfile for Quarkus application
- Configure Java runtime environment
- Set up application dependencies and build process
- Configure file storage and environment variables

## Out of Scope

- Production security hardening
- Advanced JVM tuning
- Application monitoring setup
- SSL/TLS configuration

## Clean Architecture Placement

infrastructure

## Execution Dependencies

- 0001-upload_documents_and_chat_with_private_knowledge_base-create_podman_compose_configuration.md

## Implementation Details

Create backend Dockerfile with:

1. **Multi-stage build Dockerfile**:
```dockerfile
# Build stage
FROM maven:3.9-openjdk-17-slim AS build
WORKDIR /app

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage
FROM openjdk:17-jre-slim
WORKDIR /app

# Install required packages
RUN apt-get update && apt-get install -y \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Create application user
RUN groupadd -r appuser && useradd -r -g appuser appuser

# Create directories for file storage
RUN mkdir -p /app/storage/documents && \
    chown -R appuser:appuser /app/storage

# Copy application jar
COPY --from=build /app/target/quarkus-app/ ./

# Switch to non-root user
USER appuser

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/q/health || exit 1

# Start application
CMD ["java", "-jar", "quarkus-run.jar"]
```

2. **Application configuration** (application.properties):
```properties
# Database configuration
quarkus.datasource.db-kind=postgresql
quarkus.datasource.username=${DB_USER:rag_user}
quarkus.datasource.password=${DB_PASSWORD:rag_password}
quarkus.datasource.jdbc.url=jdbc:postgresql://${DB_HOST:postgres}:${DB_PORT:5432}/${DB_NAME:rag_app}

# File storage configuration
app.storage.documents.path=${DOCUMENT_STORAGE_PATH:/app/storage/documents}
app.storage.max-file-size=${MAX_FILE_SIZE:41943040}

# Vector store configuration
app.vectorstore.url=${WEAVIATE_URL:http://weaviate:8080}
app.vectorstore.api-key=${WEAVIATE_API_KEY:}

# LLM configuration
app.llm.provider=${LLM_PROVIDER:ollama}
app.llm.url=${OLLAMA_URL:http://ollama:11434}
app.llm.model=${LLM_MODEL:llama2:7b-chat}
app.llm.timeout=${LLM_TIMEOUT:20}

# CORS configuration
quarkus.http.cors=true
quarkus.http.cors.origins=${CORS_ORIGINS:http://localhost:3000}

# Logging configuration
quarkus.log.level=${LOG_LEVEL:INFO}
quarkus.log.category."com.rag.app".level=${APP_LOG_LEVEL:DEBUG}
```

3. **Docker ignore file** (.dockerignore):
```
target/
.mvn/
mvnw
mvnw.cmd
.git/
.gitignore
README.md
*.md
.env*
docker-compose.yml
```

4. **Environment variables**:
- DB_HOST=postgres
- DB_PORT=5432
- DB_NAME=rag_app
- DB_USER=rag_user
- DB_PASSWORD=rag_password
- WEAVIATE_URL=http://weaviate:8080
- OLLAMA_URL=http://ollama:11434
- DOCUMENT_STORAGE_PATH=/app/storage/documents
- MAX_FILE_SIZE=41943040
- CORS_ORIGINS=http://localhost:3000

5. **Volume mounts**:
- Document storage: /app/storage/documents
- Application logs: /app/logs (if needed)

## Files / Modules Impacted

- backend/Dockerfile
- backend/.dockerignore
- backend/src/main/resources/application.properties
- docker-compose.yml (backend service configuration)

## Acceptance Criteria

Given backend Dockerfile is built
When container is started
Then Quarkus application should start successfully

Given environment variables are configured
When application connects to dependencies
Then database and vector store connections should work

Given file upload is attempted
When document storage is tested
Then files should be stored in mounted volume

Given health check is performed
When /q/health endpoint is called
Then application should report healthy status

## Testing Requirements

- Test Docker image build process
- Test application startup in container
- Test environment variable configuration
- Test volume mounts and file storage
- Test health check endpoint

## Dependencies / Preconditions

- Podman Compose configuration must exist
- Java 17 base image must be available
- Maven dependencies must be resolvable
- Database and vector store containers must be available