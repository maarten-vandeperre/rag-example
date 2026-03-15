# Fix Docker Compose Dev Startup Issues

## Related User Story

User Story: standardize_local_development_environment_with_podman

## Objective

Fix the docker-compose.dev.yml configuration to ensure all services start properly and reliably, addressing common startup issues and dependency problems.

## Scope

- Fix PostgreSQL container configuration and initialization
- Fix Weaviate container configuration and schema setup
- Fix Keycloak container configuration and realm import
- Fix service dependencies and startup order
- Fix volume mounts and permissions
- Add proper error handling and validation

## Out of Scope

- Production docker-compose configuration
- Performance optimization
- Advanced container security
- Multi-platform compatibility issues

## Clean Architecture Placement

infrastructure

## Execution Dependencies

- 0047-standardize_local_development_environment_with_podman-create_development_services_compose.md

## Implementation Details

Create fixed docker-compose.dev.yml:
```yaml
version: '3.8'

services:
  # PostgreSQL Database with pgvector
  postgres-dev:
    image: pgvector/pgvector:pg15
    container_name: rag-postgres-dev
    restart: unless-stopped
    environment:
      POSTGRES_DB: rag_app_dev
      POSTGRES_USER: rag_dev_user
      POSTGRES_PASSWORD: rag_dev_password
      POSTGRES_INITDB_ARGS: "--encoding=UTF-8 --lc-collate=C --lc-ctype=C"
    ports:
      - "5432:5432"
    volumes:
      - postgres_dev_data:/var/lib/postgresql/data
      - ./infrastructure/database/init-dev.sql:/docker-entrypoint-initdb.d/01-init.sql:ro
      - ./infrastructure/database/sample-dev-data.sql:/docker-entrypoint-initdb.d/02-sample-data.sql:ro
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U rag_dev_user -d rag_app_dev"]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 30s
    networks:
      - rag-dev-network

  # Weaviate Vector Database
  weaviate-dev:
    image: semitechnologies/weaviate:1.22.4
    container_name: rag-weaviate-dev
    restart: unless-stopped
    command:
      - --host
      - 0.0.0.0
      - --port
      - '8080'
      - --scheme
      - http
    environment:
      QUERY_DEFAULTS_LIMIT: 25
      AUTHENTICATION_ANONYMOUS_ACCESS_ENABLED: 'true'
      PERSISTENCE_DATA_PATH: '/var/lib/weaviate'
      DEFAULT_VECTORIZER_MODULE: 'none'
      ENABLE_MODULES: 'text2vec-transformers'
      CLUSTER_HOSTNAME: 'node1'
      ASYNC_INDEXING: 'true'
    ports:
      - "8080:8080"
    volumes:
      - weaviate_dev_data:/var/lib/weaviate
    healthcheck:
      test: ["CMD", "wget", "--no-verbose", "--tries=3", "--spider", "http://localhost:8080/v1/meta"]
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
    restart: unless-stopped
    environment:
      KEYCLOAK_ADMIN: admin
      KEYCLOAK_ADMIN_PASSWORD: admin123
      KC_DB: postgres
      KC_DB_URL: jdbc:postgresql://postgres-dev:5432/rag_app_dev
      KC_DB_USERNAME: rag_dev_user
      KC_DB_PASSWORD: rag_dev_password
      KC_HOSTNAME_STRICT: false
      KC_HOSTNAME_STRICT_HTTPS: false
      KC_HTTP_ENABLED: true
      KC_HEALTH_ENABLED: true
      KC_METRICS_ENABLED: true
    ports:
      - "8180:8080"
    volumes:
      - keycloak_dev_data:/opt/keycloak/data
      - ./infrastructure/keycloak/dev-realm.json:/opt/keycloak/data/import/realm.json:ro
    command:
      - start-dev
      - --import-realm
    depends_on:
      postgres-dev:
        condition: service_healthy
    healthcheck:
      test: ["CMD-SHELL", "exec 3<>/dev/tcp/127.0.0.1/8080;echo -e \"GET /health/ready HTTP/1.1\r\nhost: 127.0.0.1:8080\r\nConnection: close\r\n\r\n\" >&3;grep \"HTTP/1.1 200 OK\" <&3"]
      interval: 30s
      timeout: 10s
      retries: 5
      start_period: 60s
    networks:
      - rag-dev-network

  # Redis for Session Management
  redis-dev:
    image: redis:7-alpine
    container_name: rag-redis-dev
    restart: unless-stopped
    ports:
      - "6379:6379"
    volumes:
      - redis_dev_data:/data
    command: redis-server --appendonly yes --maxmemory 256mb --maxmemory-policy allkeys-lru
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 3
    networks:
      - rag-dev-network

  # Ollama for Local LLM (optional, can be disabled)
  ollama-dev:
    image: ollama/ollama:latest
    container_name: rag-ollama-dev
    restart: unless-stopped
    ports:
      - "11434:11434"
    volumes:
      - ollama_dev_models:/root/.ollama
    environment:
      OLLAMA_HOST: 0.0.0.0
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:11434/api/tags"]
      interval: 60s
      timeout: 10s
      retries: 3
      start_period: 120s
    networks:
      - rag-dev-network
    # Optional: comment out if you don't need local LLM
    profiles:
      - llm

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

Create improved startup script (start-dev-services.sh):
```bash
#!/bin/bash
set -e

echo "=== Starting RAG Application Development Services ==="

# Check if Podman/Docker is available
CONTAINER_CMD=""
if command -v podman-compose &> /dev/null; then
    CONTAINER_CMD="podman-compose"
elif command -v docker-compose &> /dev/null; then
    CONTAINER_CMD="docker-compose"
elif command -v docker &> /dev/null && docker compose version &> /dev/null; then
    CONTAINER_CMD="docker compose"
else
    echo "ERROR: Neither podman-compose nor docker-compose found."
    echo "Please install Podman with podman-compose or Docker with docker-compose."
    echo ""
    echo "Installation guides:"
    echo "  Podman: https://podman.io/getting-started/installation"
    echo "  Docker: https://docs.docker.com/get-docker/"
    exit 1
fi

echo "Using container command: $CONTAINER_CMD"

# Check if required directories exist
echo "Checking required directories..."
mkdir -p infrastructure/database
mkdir -p infrastructure/keycloak
mkdir -p infrastructure/weaviate

# Check if required files exist
REQUIRED_FILES=(
    "infrastructure/database/init-dev.sql"
    "infrastructure/database/sample-dev-data.sql"
    "infrastructure/keycloak/dev-realm.json"
)

for file in "${REQUIRED_FILES[@]}"; do
    if [ ! -f "$file" ]; then
        echo "WARNING: Required file not found: $file"
        echo "Creating placeholder file..."
        
        case "$file" in
            "infrastructure/database/init-dev.sql")
                cat > "$file" << 'EOF'
-- Development Database Initialization
CREATE EXTENSION IF NOT EXISTS vector;
SELECT 'Database initialized' as status;
EOF
                ;;
            "infrastructure/database/sample-dev-data.sql")
                cat > "$file" << 'EOF'
-- Sample Development Data
SELECT 'Sample data loaded' as status;
EOF
                ;;
            "infrastructure/keycloak/dev-realm.json")
                cat > "$file" << 'EOF'
{
  "realm": "rag-app-dev",
  "enabled": true,
  "users": [],
  "clients": []
}
EOF
                ;;
        esac
        echo "Created placeholder: $file"
    fi
done

# Load environment variables if .env.dev exists
if [ -f .env.dev ]; then
    echo "Loading environment variables from .env.dev"
    export $(cat .env.dev | grep -v '^#' | xargs)
else
    echo "No .env.dev found, using default configuration"
fi

# Stop any existing containers
echo "Stopping any existing containers..."
$CONTAINER_CMD -f docker-compose.dev.yml down --remove-orphans 2>/dev/null || true

# Start core services first (without LLM)
echo "Starting core development services..."
$CONTAINER_CMD -f docker-compose.dev.yml up -d postgres-dev redis-dev

# Wait for PostgreSQL to be ready
echo -n "Waiting for PostgreSQL to be ready..."
MAX_ATTEMPTS=30
ATTEMPT=0
while [ $ATTEMPT -lt $MAX_ATTEMPTS ]; do
    if $CONTAINER_CMD -f docker-compose.dev.yml exec -T postgres-dev pg_isready -U rag_dev_user -d rag_app_dev > /dev/null 2>&1; then
        echo " ✓"
        break
    fi
    echo -n "."
    sleep 2
    ATTEMPT=$((ATTEMPT + 1))
done

if [ $ATTEMPT -eq $MAX_ATTEMPTS ]; then
    echo " ✗"
    echo "ERROR: PostgreSQL failed to start within expected time"
    echo "Checking PostgreSQL logs:"
    $CONTAINER_CMD -f docker-compose.dev.yml logs postgres-dev
    exit 1
fi

# Start Weaviate
echo "Starting Weaviate..."
$CONTAINER_CMD -f docker-compose.dev.yml up -d weaviate-dev

# Wait for Weaviate to be ready
echo -n "Waiting for Weaviate to be ready..."
ATTEMPT=0
while [ $ATTEMPT -lt $MAX_ATTEMPTS ]; do
    if curl -f http://localhost:8080/v1/meta > /dev/null 2>&1; then
        echo " ✓"
        break
    fi
    echo -n "."
    sleep 3
    ATTEMPT=$((ATTEMPT + 1))
done

if [ $ATTEMPT -eq $MAX_ATTEMPTS ]; then
    echo " ✗"
    echo "ERROR: Weaviate failed to start within expected time"
    echo "Checking Weaviate logs:"
    $CONTAINER_CMD -f docker-compose.dev.yml logs weaviate-dev
    exit 1
fi

# Start Keycloak
echo "Starting Keycloak..."
$CONTAINER_CMD -f docker-compose.dev.yml up -d keycloak-dev

# Wait for Keycloak to be ready
echo -n "Waiting for Keycloak to be ready..."
ATTEMPT=0
MAX_KEYCLOAK_ATTEMPTS=60  # Keycloak takes longer to start
while [ $ATTEMPT -lt $MAX_KEYCLOAK_ATTEMPTS ]; do
    if curl -f http://localhost:8180/health/ready > /dev/null 2>&1; then
        echo " ✓"
        break
    fi
    echo -n "."
    sleep 5
    ATTEMPT=$((ATTEMPT + 1))
done

if [ $ATTEMPT -eq $MAX_KEYCLOAK_ATTEMPTS ]; then
    echo " ✗"
    echo "ERROR: Keycloak failed to start within expected time"
    echo "Checking Keycloak logs:"
    $CONTAINER_CMD -f docker-compose.dev.yml logs keycloak-dev
    exit 1
fi

# Optionally start LLM service
if [ "${START_LLM:-false}" = "true" ]; then
    echo "Starting Ollama LLM service..."
    $CONTAINER_CMD -f docker-compose.dev.yml --profile llm up -d ollama-dev
    
    echo -n "Waiting for Ollama to be ready..."
    ATTEMPT=0
    while [ $ATTEMPT -lt 30 ]; do
        if curl -f http://localhost:11434/api/tags > /dev/null 2>&1; then
            echo " ✓"
            break
        fi
        echo -n "."
        sleep 5
        ATTEMPT=$((ATTEMPT + 1))
    done
    
    if [ $ATTEMPT -eq 30 ]; then
        echo " ✗"
        echo "WARNING: Ollama failed to start, but continuing..."
    fi
fi

echo ""
echo "=== Development Services Ready ==="
echo "PostgreSQL:  localhost:5432 (rag_dev_user/rag_dev_password)"
echo "Weaviate:    http://localhost:8080"
echo "Keycloak:    http://localhost:8180 (admin/admin123)"
echo "Redis:       localhost:6379"
if [ "${START_LLM:-false}" = "true" ]; then
    echo "Ollama:      http://localhost:11434"
fi
echo ""
echo "Service Status:"
$CONTAINER_CMD -f docker-compose.dev.yml ps

echo ""
echo "You can now start the backend and frontend separately:"
echo "  Backend:  cd backend && ./start-dev.sh"
echo "  Frontend: cd frontend && ./start-dev.sh"
echo ""
echo "To stop services: ./stop-dev-services.sh"
echo "To check status: ./status-dev-services.sh"
```

Create improved status script (status-dev-services.sh):
```bash
#!/bin/bash

echo "=== RAG Application Development Services Status ==="

# Determine container command
CONTAINER_CMD=""
if command -v podman-compose &> /dev/null; then
    CONTAINER_CMD="podman-compose"
elif command -v docker-compose &> /dev/null; then
    CONTAINER_CMD="docker-compose"
elif command -v docker &> /dev/null && docker compose version &> /dev/null; then
    CONTAINER_CMD="docker compose"
else
    echo "ERROR: No container orchestration tool found"
    exit 1
fi

# Check if compose file exists
if [ ! -f docker-compose.dev.yml ]; then
    echo "ERROR: docker-compose.dev.yml not found"
    exit 1
fi

# Show service status
echo "Container Status:"
$CONTAINER_CMD -f docker-compose.dev.yml ps

echo ""
echo "=== Service Health Checks ==="

# PostgreSQL
echo -n "PostgreSQL: "
if pg_isready -h localhost -p 5432 -U rag_dev_user > /dev/null 2>&1; then
    echo "✓ Running and accepting connections"
else
    echo "✗ Not accessible"
fi

# Weaviate
echo -n "Weaviate:   "
if curl -s http://localhost:8080/v1/meta > /dev/null 2>&1; then
    echo "✓ Running and accessible"
else
    echo "✗ Not accessible"
fi

# Keycloak
echo -n "Keycloak:   "
if curl -s http://localhost:8180/health/ready > /dev/null 2>&1; then
    echo "✓ Running and ready"
else
    echo "✗ Not accessible or not ready"
fi

# Redis
echo -n "Redis:      "
if redis-cli -h localhost -p 6379 ping > /dev/null 2>&1; then
    echo "✓ Running and responding"
else
    echo "✗ Not accessible"
fi

# Ollama (optional)
echo -n "Ollama:     "
if curl -s http://localhost:11434/api/tags > /dev/null 2>&1; then
    echo "✓ Running and accessible"
else
    echo "- Not running (optional service)"
fi

echo ""
echo "=== Resource Usage ==="
if command -v podman &> /dev/null; then
    podman stats --no-stream --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.MemPerc}}"
elif command -v docker &> /dev/null; then
    docker stats --no-stream --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.MemPerc}}"
fi

echo ""
echo "=== Quick Actions ==="
echo "Start services:     ./start-dev-services.sh"
echo "Stop services:      ./stop-dev-services.sh"
echo "View logs:          $CONTAINER_CMD -f docker-compose.dev.yml logs [service-name]"
echo "Restart service:    $CONTAINER_CMD -f docker-compose.dev.yml restart [service-name]"
```

Create improved stop script (stop-dev-services.sh):
```bash
#!/bin/bash

echo "=== Stopping RAG Application Development Services ==="

# Determine container command
CONTAINER_CMD=""
if command -v podman-compose &> /dev/null; then
    CONTAINER_CMD="podman-compose"
elif command -v docker-compose &> /dev/null; then
    CONTAINER_CMD="docker-compose"
elif command -v docker &> /dev/null && docker compose version &> /dev/null; then
    CONTAINER_CMD="docker compose"
else
    echo "ERROR: No container orchestration tool found"
    exit 1
fi

# Stop and remove containers
echo "Stopping containers..."
$CONTAINER_CMD -f docker-compose.dev.yml down --remove-orphans

# Optionally remove volumes (ask user)
if [ "$1" = "--clean" ] || [ "$1" = "-c" ]; then
    echo "Removing volumes (data will be lost)..."
    $CONTAINER_CMD -f docker-compose.dev.yml down --volumes
    echo "✓ All data removed"
else
    echo "✓ Services stopped (data preserved)"
    echo ""
    echo "To also remove all data, use: $0 --clean"
fi

echo ""
echo "To start services again: ./start-dev-services.sh"
```

Create troubleshooting script (troubleshoot-dev-services.sh):
```bash
#!/bin/bash

echo "=== RAG Development Services Troubleshooting ==="

# Determine container command
CONTAINER_CMD=""
if command -v podman-compose &> /dev/null; then
    CONTAINER_CMD="podman-compose"
elif command -v docker-compose &> /dev/null; then
    CONTAINER_CMD="docker-compose"
elif command -v docker &> /dev/null && docker compose version &> /dev/null; then
    CONTAINER_CMD="docker compose"
else
    echo "ERROR: No container orchestration tool found"
    exit 1
fi

echo "Container Command: $CONTAINER_CMD"
echo ""

# Check system resources
echo "=== System Resources ==="
echo "Available Memory:"
free -h 2>/dev/null || echo "Memory info not available"
echo ""
echo "Available Disk Space:"
df -h . 2>/dev/null || echo "Disk info not available"
echo ""

# Check port availability
echo "=== Port Availability ==="
PORTS=(5432 8080 8180 6379 11434)
for port in "${PORTS[@]}"; do
    echo -n "Port $port: "
    if netstat -tuln 2>/dev/null | grep ":$port " > /dev/null; then
        echo "IN USE"
        echo "  Process using port:"
        lsof -i ":$port" 2>/dev/null | head -2 || echo "  Unable to determine process"
    else
        echo "Available"
    fi
done
echo ""

# Check container status
echo "=== Container Status ==="
$CONTAINER_CMD -f docker-compose.dev.yml ps
echo ""

# Check container logs for errors
echo "=== Recent Container Logs (Last 10 lines) ==="
SERVICES=(postgres-dev weaviate-dev keycloak-dev redis-dev)
for service in "${SERVICES[@]}"; do
    echo "--- $service ---"
    $CONTAINER_CMD -f docker-compose.dev.yml logs --tail 10 "$service" 2>/dev/null || echo "No logs available"
    echo ""
done

# Check network connectivity
echo "=== Network Connectivity ==="
echo -n "PostgreSQL: "
if pg_isready -h localhost -p 5432 -U rag_dev_user > /dev/null 2>&1; then
    echo "✓ Connected"
else
    echo "✗ Connection failed"
    echo "  Trying to connect..."
    pg_isready -h localhost -p 5432 -U rag_dev_user 2>&1 | head -3
fi

echo -n "Weaviate: "
if curl -s --max-time 5 http://localhost:8080/v1/meta > /dev/null 2>&1; then
    echo "✓ Connected"
else
    echo "✗ Connection failed"
    echo "  Trying to connect..."
    curl -s --max-time 5 http://localhost:8080/v1/meta 2>&1 | head -3
fi

echo -n "Keycloak: "
if curl -s --max-time 5 http://localhost:8180/health/ready > /dev/null 2>&1; then
    echo "✓ Connected"
else
    echo "✗ Connection failed"
    echo "  Trying to connect..."
    curl -s --max-time 5 http://localhost:8180/health/ready 2>&1 | head -3
fi

echo ""
echo "=== Recommended Actions ==="
echo "1. Check if ports are available (see Port Availability above)"
echo "2. Ensure sufficient system resources (memory/disk)"
echo "3. Check container logs for specific errors:"
echo "   $CONTAINER_CMD -f docker-compose.dev.yml logs [service-name]"
echo "4. Try restarting specific service:"
echo "   $CONTAINER_CMD -f docker-compose.dev.yml restart [service-name]"
echo "5. Try full restart:"
echo "   ./stop-dev-services.sh && ./start-dev-services.sh"
echo "6. If issues persist, try clean restart:"
echo "   ./stop-dev-services.sh --clean && ./start-dev-services.sh"
echo ""
echo "For escalation, include the output of this script."
```

Create environment file template (.env.dev.template):
```bash
# Development Environment Configuration Template
# Copy this file to .env.dev and customize as needed

# Compose Project Name
COMPOSE_PROJECT_NAME=rag-dev

# Database Configuration
POSTGRES_DB=rag_app_dev
POSTGRES_USER=rag_dev_user
POSTGRES_PASSWORD=rag_dev_password

# Keycloak Configuration
KEYCLOAK_ADMIN=admin
KEYCLOAK_ADMIN_PASSWORD=admin123

# Optional: Start LLM service
START_LLM=false

# Optional: Custom ports (if defaults conflict)
# POSTGRES_PORT=5432
# WEAVIATE_PORT=8080
# KEYCLOAK_PORT=8180
# REDIS_PORT=6379
# OLLAMA_PORT=11434

# Optional: Resource limits
# POSTGRES_MEMORY=512m
# WEAVIATE_MEMORY=1g
# KEYCLOAK_MEMORY=1g
```

## Files / Modules Impacted

- docker-compose.dev.yml (complete rewrite)
- start-dev-services.sh (improved with better error handling)
- stop-dev-services.sh (improved with clean option)
- status-dev-services.sh (enhanced with health checks)
- troubleshoot-dev-services.sh (new troubleshooting script)
- .env.dev.template (new environment template)

## Acceptance Criteria

Given the fixed docker-compose configuration
When ./start-dev-services.sh is executed
Then all services should start successfully without errors

Given services are starting
When there are startup issues
Then clear error messages should be displayed with troubleshooting guidance

Given services are running
When ./status-dev-services.sh is executed
Then accurate status information should be displayed for all services

Given there are service issues
When ./troubleshoot-dev-services.sh is executed
Then comprehensive diagnostic information should be provided

## Testing Requirements

- Test startup on clean system
- Test startup with port conflicts
- Test startup with insufficient resources
- Test service health checks
- Test error handling and recovery

## Dependencies / Preconditions

- Podman/Docker must be installed and running
- Required ports must be available
- Sufficient system resources (memory/disk)
- Required configuration files must exist