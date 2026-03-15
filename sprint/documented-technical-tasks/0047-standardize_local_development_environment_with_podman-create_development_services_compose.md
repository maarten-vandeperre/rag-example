# Create Development Services Compose

## Related User Story

User Story: standardize_local_development_environment_with_podman

## Objective

Create a Podman Compose configuration specifically for supporting services needed in local development, excluding the application backend and frontend which will run separately.

## Scope

- Create docker-compose.dev.yml for development supporting services
- Configure PostgreSQL database for local development
- Configure Weaviate vector database for local development
- Configure Keycloak for authentication in local development
- Set up service networking and data persistence
- Create development-specific environment configuration

## Out of Scope

- Backend application containerization (runs separately)
- Frontend application containerization (runs separately)
- Production deployment configurations
- CI/CD pipeline integration

## Clean Architecture Placement

infrastructure

## Execution Dependencies

None

## Implementation Details

Create docker-compose.dev.yml for development services:
```yaml
version: '3.8'

services:
  # PostgreSQL Database
  postgres-dev:
    image: postgres:15-alpine
    container_name: rag-postgres-dev
    environment:
      POSTGRES_DB: rag_app_dev
      POSTGRES_USER: rag_dev_user
      POSTGRES_PASSWORD: rag_dev_password
      POSTGRES_INITDB_ARGS: "--encoding=UTF-8"
    ports:
      - "5432:5432"
    volumes:
      - postgres_dev_data:/var/lib/postgresql/data
      - ./infrastructure/database/init-dev.sql:/docker-entrypoint-initdb.d/01-init.sql
      - ./infrastructure/database/sample-dev-data.sql:/docker-entrypoint-initdb.d/02-sample-data.sql
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U rag_dev_user -d rag_app_dev"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - rag-dev-network

  # Weaviate Vector Database
  weaviate-dev:
    image: semitechnologies/weaviate:1.22.4
    container_name: rag-weaviate-dev
    environment:
      QUERY_DEFAULTS_LIMIT: 25
      AUTHENTICATION_ANONYMOUS_ACCESS_ENABLED: 'true'
      PERSISTENCE_DATA_PATH: '/var/lib/weaviate'
      DEFAULT_VECTORIZER_MODULE: 'none'
      ENABLE_MODULES: 'text2vec-transformers'
      CLUSTER_HOSTNAME: 'node1'
    ports:
      - "8080:8080"
    volumes:
      - weaviate_dev_data:/var/lib/weaviate
      - ./infrastructure/weaviate/dev-schema.json:/schema/schema.json
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/v1/meta"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 40s
    networks:
      - rag-dev-network

  # Keycloak Authentication
  keycloak-dev:
    image: quay.io/keycloak/keycloak:23.0
    container_name: rag-keycloak-dev
    environment:
      KEYCLOAK_ADMIN: admin
      KEYCLOAK_ADMIN_PASSWORD: admin123
      KC_DB: postgres
      KC_DB_URL: jdbc:postgresql://postgres-dev:5432/rag_app_dev
      KC_DB_USERNAME: rag_dev_user
      KC_DB_PASSWORD: rag_dev_password
      KC_HOSTNAME_STRICT: false
      KC_HTTP_ENABLED: true
    ports:
      - "8180:8080"
    volumes:
      - keycloak_dev_data:/opt/keycloak/data
      - ./infrastructure/keycloak/dev-realm.json:/opt/keycloak/data/import/realm.json
    command:
      - start-dev
      - --import-realm
    depends_on:
      postgres-dev:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/health/ready"]
      interval: 30s
      timeout: 10s
      retries: 5
      start_period: 60s
    networks:
      - rag-dev-network

  # Redis for Session Management (optional)
  redis-dev:
    image: redis:7-alpine
    container_name: rag-redis-dev
    ports:
      - "6379:6379"
    volumes:
      - redis_dev_data:/data
    command: redis-server --appendonly yes
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 3
    networks:
      - rag-dev-network

  # Ollama for Local LLM (optional)
  ollama-dev:
    image: ollama/ollama:latest
    container_name: rag-ollama-dev
    ports:
      - "11434:11434"
    volumes:
      - ollama_dev_models:/root/.ollama
    environment:
      OLLAMA_HOST: 0.0.0.0
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:11434/api/tags"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s
    networks:
      - rag-dev-network

volumes:
  postgres_dev_data:
    driver: local
  weaviate_dev_data:
    driver: local
  keycloak_dev_data:
    driver: local
  redis_dev_data:
    driver: local
  ollama_dev_models:
    driver: local

networks:
  rag-dev-network:
    driver: bridge
    ipam:
      config:
        - subnet: 172.20.0.0/16
```

Create .env.dev for development environment:
```bash
# Development Environment Configuration
COMPOSE_PROJECT_NAME=rag-dev

# Database Configuration
DB_HOST=localhost
DB_PORT=5432
DB_NAME=rag_app_dev
DB_USER=rag_dev_user
DB_PASSWORD=rag_dev_password
DB_URL=jdbc:postgresql://localhost:5432/rag_app_dev

# Vector Database Configuration
WEAVIATE_URL=http://localhost:8080
WEAVIATE_API_KEY=

# Authentication Configuration
KEYCLOAK_URL=http://localhost:8180
KEYCLOAK_REALM=rag-app-dev
KEYCLOAK_CLIENT_ID=rag-app-client
KEYCLOAK_CLIENT_SECRET=dev-secret

# LLM Configuration
OLLAMA_URL=http://localhost:11434
LLM_MODEL=llama2:7b-chat

# Redis Configuration
REDIS_URL=redis://localhost:6379

# Application Configuration (for native development)
BACKEND_PORT=8081
FRONTEND_PORT=3000
API_BASE_URL=http://localhost:8081/api

# Development Flags
ENVIRONMENT=development
DEBUG_MODE=true
LOG_LEVEL=DEBUG
```

Create development startup script (start-dev-services.sh):
```bash
#!/bin/bash
set -e

echo "=== Starting RAG Application Development Services ==="

# Check if Podman is available
if ! command -v podman-compose &> /dev/null; then
    echo "ERROR: podman-compose not found. Please install Podman and podman-compose."
    echo "Installation guide: https://podman.io/getting-started/installation"
    exit 1
fi

# Load development environment
if [ -f .env.dev ]; then
    export $(cat .env.dev | grep -v '^#' | xargs)
    echo "✓ Loaded development environment configuration"
else
    echo "WARNING: .env.dev not found. Using default configuration."
fi

# Start development services
echo "Starting development supporting services..."
podman-compose -f docker-compose.dev.yml --env-file .env.dev up -d

# Wait for services to be healthy
echo "Waiting for services to be ready..."

# Wait for PostgreSQL
echo -n "Waiting for PostgreSQL..."
until podman exec rag-postgres-dev pg_isready -U rag_dev_user -d rag_app_dev > /dev/null 2>&1; do
    echo -n "."
    sleep 2
done
echo " ✓"

# Wait for Weaviate
echo -n "Waiting for Weaviate..."
until curl -f http://localhost:8080/v1/meta > /dev/null 2>&1; do
    echo -n "."
    sleep 2
done
echo " ✓"

# Wait for Keycloak
echo -n "Waiting for Keycloak..."
until curl -f http://localhost:8180/health/ready > /dev/null 2>&1; do
    echo -n "."
    sleep 5
done
echo " ✓"

echo ""
echo "=== Development Services Ready ==="
echo "PostgreSQL:  localhost:5432"
echo "Weaviate:    http://localhost:8080"
echo "Keycloak:    http://localhost:8180 (admin/admin123)"
echo "Redis:       localhost:6379"
echo "Ollama:      http://localhost:11434"
echo ""
echo "You can now start the backend and frontend separately:"
echo "  Backend:  ./gradlew :backend:dev"
echo "  Frontend: ./gradlew :frontend:dev"
echo ""
echo "To stop services: ./stop-dev-services.sh"
```

Create development stop script (stop-dev-services.sh):
```bash
#!/bin/bash
set -e

echo "=== Stopping RAG Application Development Services ==="

# Stop and remove containers
podman-compose -f docker-compose.dev.yml down

echo "✓ Development services stopped"
echo ""
echo "To start services again: ./start-dev-services.sh"
```

Create development status script (status-dev-services.sh):
```bash
#!/bin/bash

echo "=== RAG Application Development Services Status ==="

# Check if compose file exists
if [ ! -f docker-compose.dev.yml ]; then
    echo "ERROR: docker-compose.dev.yml not found"
    exit 1
fi

# Show service status
podman-compose -f docker-compose.dev.yml ps

echo ""
echo "=== Service Health Checks ==="

# PostgreSQL
if curl -s http://localhost:5432 > /dev/null 2>&1; then
    echo "PostgreSQL: ✓ Running"
else
    echo "PostgreSQL: ✗ Not accessible"
fi

# Weaviate
if curl -s http://localhost:8080/v1/meta > /dev/null 2>&1; then
    echo "Weaviate:   ✓ Running"
else
    echo "Weaviate:   ✗ Not accessible"
fi

# Keycloak
if curl -s http://localhost:8180/health/ready > /dev/null 2>&1; then
    echo "Keycloak:   ✓ Running"
else
    echo "Keycloak:   ✗ Not accessible"
fi

# Redis
if redis-cli -h localhost ping > /dev/null 2>&1; then
    echo "Redis:      ✓ Running"
else
    echo "Redis:      ✗ Not accessible"
fi
```

## Files / Modules Impacted

- docker-compose.dev.yml
- .env.dev
- start-dev-services.sh
- stop-dev-services.sh
- status-dev-services.sh
- infrastructure/database/init-dev.sql
- infrastructure/keycloak/dev-realm.json
- infrastructure/weaviate/dev-schema.json

## Acceptance Criteria

Given the development services compose is created
When ./start-dev-services.sh is executed
Then all supporting services should start and be accessible

Given the supporting services are running
When health checks are performed
Then PostgreSQL, Weaviate, and Keycloak should be healthy

Given the development environment is configured
When developers need database access
Then they should be able to connect to localhost:5432

Given the services are started
When ./status-dev-services.sh is executed
Then the status of all services should be displayed

## Testing Requirements

- Test service startup and health checks
- Test service networking and accessibility
- Test data persistence across restarts
- Test error handling for failed services
- Test script execution on different platforms

## Dependencies / Preconditions

- Podman and podman-compose must be installed
- Required ports (5432, 8080, 8180, 6379, 11434) must be available
- Sufficient system resources for running containers