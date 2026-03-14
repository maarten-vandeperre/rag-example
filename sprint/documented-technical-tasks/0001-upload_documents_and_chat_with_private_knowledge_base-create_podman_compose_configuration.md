# Create Podman Compose configuration

## Related User Story

User Story: upload_documents_and_chat_with_private_knowledge_base

## Objective

Create Podman Compose configuration to orchestrate all required services for the RAG application including database, vector store, backend, and frontend.

## Scope

- Create docker-compose.yml for Podman Compose
- Define all required services and their dependencies
- Configure service networking and communication
- Set up volume mounts for persistent data
- Define environment variables and configuration

## Out of Scope

- Production deployment configurations
- Kubernetes manifests
- CI/CD pipeline setup
- Monitoring and logging setup

## Clean Architecture Placement

infrastructure

## Execution Dependencies

None

## Implementation Details

Create docker-compose.yml with services:

1. **PostgreSQL Database**:
   - Image: postgres:15-alpine with pgvector extension
   - Environment: database name, user, password
   - Volume: persistent database storage
   - Port: 5432 (internal)

2. **Vector Database** (Weaviate):
   - Image: semitechnologies/weaviate:latest
   - Environment: authentication disabled for development
   - Volume: persistent vector data
   - Port: 8080 (internal)

3. **Backend Service**:
   - Build context: ./backend
   - Depends on: postgres, weaviate
   - Environment: database connection, vector store URL
   - Port: 8080 (external)
   - Volume: document storage

4. **Frontend Service**:
   - Build context: ./frontend
   - Depends on: backend
   - Environment: backend API URL
   - Port: 3000 (external)

5. **LLM Service** (Ollama for local LLM):
   - Image: ollama/ollama:latest
   - Volume: model storage
   - Port: 11434 (internal)

Network configuration:
- Create custom bridge network for service communication
- Expose only necessary ports to host
- Use service names for internal communication

Volume configuration:
- postgres-data: database persistence
- weaviate-data: vector store persistence
- document-storage: uploaded files
- ollama-models: LLM models

Environment variables:
- Database credentials
- API keys (if using external LLM)
- Application configuration
- Development vs production flags

## Files / Modules Impacted

- docker-compose.yml
- .env.example
- .env.development

## Acceptance Criteria

Given Podman Compose is installed
When docker-compose up is executed
Then all services should start successfully

Given all services are running
When health checks are performed
Then all services should be healthy and accessible

Given the application is started
When frontend is accessed on port 3000
Then the application should load successfully

Given services are stopped
When docker-compose down is executed
Then all services should stop cleanly with data preserved

## Testing Requirements

- Test service startup and shutdown
- Test service health checks
- Test inter-service communication
- Test volume persistence after restart
- Test environment variable configuration

## Dependencies / Preconditions

- Podman and Podman Compose must be installed
- Required ports (3000, 8080, 5432) must be available