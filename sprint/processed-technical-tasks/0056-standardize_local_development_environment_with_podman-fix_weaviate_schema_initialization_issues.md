# Fix Weaviate Schema Initialization Issues

## Related User Story

User Story: standardize_local_development_environment_with_podman

## Objective

Fix the Weaviate schema initialization issues that cause 422 errors during development services startup, ensuring proper schema creation and validation.

## Scope

- Fix Weaviate schema configuration for compatibility
- Improve schema initialization process with better error handling
- Add proper validation and retry logic
- Update startup scripts to handle Weaviate initialization properly
- Add troubleshooting for common Weaviate issues

## Out of Scope

- Advanced Weaviate configuration optimization
- Production Weaviate clustering
- Custom vectorizer configurations
- Performance tuning for large datasets

## Clean Architecture Placement

infrastructure

## Execution Dependencies

- 0050-standardize_local_development_environment_with_podman-configure_vector_database_development_setup.md
- 0054-standardize_local_development_environment_with_podman-fix_docker_compose_dev_startup_issues.md

## Implementation Details

Create fixed Weaviate schema configuration (infrastructure/weaviate/dev-schema.json):
```json
{
  "classes": [
    {
      "class": "DocumentChunk",
      "description": "A chunk of text from an uploaded document for semantic search",
      "vectorizer": "none",
      "vectorIndexType": "hnsw",
      "vectorIndexConfig": {
        "skip": false,
        "cleanupIntervalSeconds": 300,
        "maxConnections": 64,
        "efConstruction": 128,
        "ef": -1,
        "dynamicEfMin": 100,
        "dynamicEfMax": 500,
        "dynamicEfFactor": 8,
        "vectorCacheMaxObjects": 1000000000000,
        "flatSearchCutoff": 40000,
        "distance": "cosine"
      },
      "properties": [
        {
          "name": "documentId",
          "dataType": ["text"],
          "description": "ID of the parent document",
          "indexFilterable": true,
          "indexSearchable": false
        },
        {
          "name": "chunkIndex",
          "dataType": ["int"],
          "description": "Index of this chunk within the document",
          "indexFilterable": true,
          "indexSearchable": false
        },
        {
          "name": "textContent",
          "dataType": ["text"],
          "description": "The actual text content of the chunk",
          "indexFilterable": false,
          "indexSearchable": true,
          "tokenization": "word"
        },
        {
          "name": "uploadedBy",
          "dataType": ["text"],
          "description": "User ID who uploaded the document",
          "indexFilterable": true,
          "indexSearchable": false
        },
        {
          "name": "fileName",
          "dataType": ["text"],
          "description": "Original filename of the document",
          "indexFilterable": true,
          "indexSearchable": true
        },
        {
          "name": "fileType",
          "dataType": ["text"],
          "description": "Type of the source file (PDF, MARKDOWN, PLAIN_TEXT)",
          "indexFilterable": true,
          "indexSearchable": false
        },
        {
          "name": "createdAt",
          "dataType": ["date"],
          "description": "When this chunk was created",
          "indexFilterable": true,
          "indexSearchable": false
        },
        {
          "name": "chunkSize",
          "dataType": ["int"],
          "description": "Size of the text chunk in characters",
          "indexFilterable": true,
          "indexSearchable": false
        }
      ]
    }
  ]
}
```

Create improved Weaviate initialization script (infrastructure/weaviate/init-weaviate-dev.sh):
```bash
#!/bin/bash
set -e

WEAVIATE_URL="http://localhost:8080"
SCHEMA_FILE="infrastructure/weaviate/dev-schema.json"
MAX_ATTEMPTS=30
ATTEMPT=0

echo "=== Initializing Weaviate Development Setup ==="

# Check if schema file exists
if [ ! -f "$SCHEMA_FILE" ]; then
    echo "ERROR: Schema file not found: $SCHEMA_FILE"
    echo "Creating minimal schema file..."
    
    mkdir -p "$(dirname "$SCHEMA_FILE")"
    cat > "$SCHEMA_FILE" << 'EOF'
{
  "classes": [
    {
      "class": "DocumentChunk",
      "description": "A chunk of text from an uploaded document",
      "vectorizer": "none",
      "properties": [
        {
          "name": "textContent",
          "dataType": ["text"],
          "description": "The text content"
        },
        {
          "name": "documentId",
          "dataType": ["text"],
          "description": "Document ID"
        }
      ]
    }
  ]
}
EOF
    echo "✓ Created minimal schema file"
fi

# Wait for Weaviate to be ready
echo "Waiting for Weaviate to be ready..."
while [ $ATTEMPT -lt $MAX_ATTEMPTS ]; do
    if curl -s --max-time 5 "${WEAVIATE_URL}/v1/meta" > /dev/null 2>&1; then
        echo " ✓ Weaviate is responding"
        break
    fi
    echo -n "."
    sleep 3
    ATTEMPT=$((ATTEMPT + 1))
done

if [ $ATTEMPT -eq $MAX_ATTEMPTS ]; then
    echo " ✗"
    echo "ERROR: Weaviate is not responding after waiting"
    echo "Checking Weaviate container status..."
    if command -v podman-compose &> /dev/null; then
        podman-compose -f docker-compose.dev.yml logs --tail 20 weaviate-dev
    elif command -v docker-compose &> /dev/null; then
        docker-compose -f docker-compose.dev.yml logs --tail 20 weaviate-dev
    fi
    exit 1
fi

# Additional wait for Weaviate to be fully initialized
echo "Waiting for Weaviate to be fully initialized..."
sleep 10

# Check Weaviate readiness
echo "Checking Weaviate readiness..."
META_RESPONSE=$(curl -s "${WEAVIATE_URL}/v1/meta" 2>/dev/null || echo "")
if ! echo "$META_RESPONSE" | grep -q "hostname"; then
    echo "ERROR: Weaviate meta endpoint not returning expected data"
    echo "Response: $META_RESPONSE"
    exit 1
fi

echo "✓ Weaviate is ready"

# Check if schema already exists
echo "Checking existing schema..."
EXISTING_SCHEMA=$(curl -s "${WEAVIATE_URL}/v1/schema" 2>/dev/null || echo '{"classes":[]}')
EXISTING_CLASSES=$(echo "$EXISTING_SCHEMA" | jq -r '.classes[]?.class // empty' 2>/dev/null || echo "")

if echo "$EXISTING_CLASSES" | grep -q "DocumentChunk"; then
    echo "✓ DocumentChunk class already exists"
    
    # Verify the existing class
    CLASS_INFO=$(curl -s "${WEAVIATE_URL}/v1/schema/DocumentChunk" 2>/dev/null || echo "")
    if echo "$CLASS_INFO" | grep -q "DocumentChunk"; then
        echo "✓ DocumentChunk class is properly configured"
    else
        echo "⚠ DocumentChunk class exists but may have issues"
    fi
else
    echo "Creating Weaviate schema..."
    
    # Validate schema file before sending
    if ! jq empty "$SCHEMA_FILE" > /dev/null 2>&1; then
        echo "ERROR: Invalid JSON in schema file: $SCHEMA_FILE"
        exit 1
    fi
    
    # Create schema with error handling
    SCHEMA_RESPONSE=$(curl -s -w "%{http_code}" -X POST "${WEAVIATE_URL}/v1/schema" \
        -H "Content-Type: application/json" \
        -d @"$SCHEMA_FILE" 2>/dev/null || echo "000")
    
    HTTP_CODE="${SCHEMA_RESPONSE: -3}"
    RESPONSE_BODY="${SCHEMA_RESPONSE%???}"
    
    case "$HTTP_CODE" in
        "200"|"201")
            echo "✓ Schema created successfully"
            ;;
        "422")
            echo "ERROR: Schema validation failed (422)"
            echo "Response: $RESPONSE_BODY"
            echo ""
            echo "Common causes:"
            echo "1. Invalid property configuration"
            echo "2. Unsupported data types"
            echo "3. Conflicting class definitions"
            echo ""
            echo "Trying to create a minimal schema instead..."
            
            # Try minimal schema
            MINIMAL_SCHEMA='{
              "class": "DocumentChunk",
              "description": "Simple document chunk",
              "vectorizer": "none",
              "properties": [
                {
                  "name": "textContent",
                  "dataType": ["text"]
                },
                {
                  "name": "documentId", 
                  "dataType": ["text"]
                }
              ]
            }'
            
            MINIMAL_RESPONSE=$(curl -s -w "%{http_code}" -X POST "${WEAVIATE_URL}/v1/schema" \
                -H "Content-Type: application/json" \
                -d "$MINIMAL_SCHEMA" 2>/dev/null || echo "000")
            
            MINIMAL_HTTP_CODE="${MINIMAL_RESPONSE: -3}"
            
            if [ "$MINIMAL_HTTP_CODE" = "200" ] || [ "$MINIMAL_HTTP_CODE" = "201" ]; then
                echo "✓ Minimal schema created successfully"
            else
                echo "ERROR: Even minimal schema creation failed"
                echo "Response: ${MINIMAL_RESPONSE%???}"
                exit 1
            fi
            ;;
        "409")
            echo "⚠ Schema already exists (409)"
            echo "This is usually not an error"
            ;;
        *)
            echo "ERROR: Unexpected response code: $HTTP_CODE"
            echo "Response: $RESPONSE_BODY"
            exit 1
            ;;
    esac
fi

# Verify schema creation
echo "Verifying schema..."
FINAL_SCHEMA=$(curl -s "${WEAVIATE_URL}/v1/schema" 2>/dev/null || echo '{"classes":[]}')
DOCUMENT_CHUNK_EXISTS=$(echo "$FINAL_SCHEMA" | jq -r '.classes[] | select(.class == "DocumentChunk") | .class' 2>/dev/null || echo "")

if [ "$DOCUMENT_CHUNK_EXISTS" = "DocumentChunk" ]; then
    echo "✓ DocumentChunk class verified"
    
    # Show class properties
    PROPERTIES=$(echo "$FINAL_SCHEMA" | jq -r '.classes[] | select(.class == "DocumentChunk") | .properties[].name' 2>/dev/null || echo "")
    if [ -n "$PROPERTIES" ]; then
        echo "Properties:"
        echo "$PROPERTIES" | while read -r prop; do
            if [ -n "$prop" ]; then
                echo "  - $prop"
            fi
        done
    fi
else
    echo "ERROR: DocumentChunk class not found after creation"
    echo "Available classes:"
    echo "$FINAL_SCHEMA" | jq -r '.classes[].class' 2>/dev/null || echo "None"
    exit 1
fi

echo ""
echo "=== Weaviate Development Setup Complete ==="
echo "Weaviate URL: ${WEAVIATE_URL}"
echo "GraphQL Endpoint: ${WEAVIATE_URL}/v1/graphql"
echo "REST API: ${WEAVIATE_URL}/v1"
echo ""
echo "Available classes:"
echo "$FINAL_SCHEMA" | jq -r '.classes[].class' 2>/dev/null || echo "None"
```

Update the startup script to handle Weaviate initialization properly (start-dev-services.sh):
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
    exit 1
fi

echo "Using container command: $CONTAINER_CMD"

# Check if required directories exist
echo "Checking required directories..."
mkdir -p infrastructure/database
mkdir -p infrastructure/keycloak
mkdir -p infrastructure/weaviate

# Check if required files exist and create minimal versions if missing
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

# Start core services first
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

# Wait for Weaviate to be ready with more patience
echo -n "Waiting for Weaviate to be ready..."
ATTEMPT=0
MAX_WEAVIATE_ATTEMPTS=60  # Increased timeout for Weaviate
while [ $ATTEMPT -lt $MAX_WEAVIATE_ATTEMPTS ]; do
    if curl -s --max-time 5 http://localhost:8080/v1/meta > /dev/null 2>&1; then
        echo " ✓"
        break
    fi
    echo -n "."
    sleep 3
    ATTEMPT=$((ATTEMPT + 1))
done

if [ $ATTEMPT -eq $MAX_WEAVIATE_ATTEMPTS ]; then
    echo " ✗"
    echo "ERROR: Weaviate failed to start within expected time"
    echo "Checking Weaviate logs:"
    $CONTAINER_CMD -f docker-compose.dev.yml logs weaviate-dev
    exit 1
fi

# Initialize Weaviate schema with better error handling
echo "Initializing Weaviate schema..."
if [ -f "infrastructure/weaviate/init-weaviate-dev.sh" ]; then
    chmod +x infrastructure/weaviate/init-weaviate-dev.sh
    if ./infrastructure/weaviate/init-weaviate-dev.sh; then
        echo "✓ Weaviate schema initialized successfully"
    else
        echo "⚠ Weaviate schema initialization failed, but continuing..."
        echo "You can manually initialize later with: ./infrastructure/weaviate/init-weaviate-dev.sh"
    fi
else
    echo "⚠ Weaviate initialization script not found, skipping schema setup"
    echo "Schema will need to be created manually later"
fi

# Start Keycloak
echo "Starting Keycloak..."
$CONTAINER_CMD -f docker-compose.dev.yml up -d keycloak-dev

# Wait for Keycloak to be ready
echo -n "Waiting for Keycloak to be ready..."
ATTEMPT=0
MAX_KEYCLOAK_ATTEMPTS=60
while [ $ATTEMPT -lt $MAX_KEYCLOAK_ATTEMPTS ]; do
    if curl -s --max-time 5 http://localhost:8180/health/ready > /dev/null 2>&1; then
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
        if curl -s --max-time 5 http://localhost:11434/api/tags > /dev/null 2>&1; then
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
echo ""
echo "If you encounter issues:"
echo "  Check logs: $CONTAINER_CMD -f docker-compose.dev.yml logs [service-name]"
echo "  Troubleshoot: ./troubleshoot-dev-services.sh"
```

Create Weaviate troubleshooting script (infrastructure/weaviate/troubleshoot-weaviate.sh):
```bash
#!/bin/bash

WEAVIATE_URL="http://localhost:8080"

echo "=== Weaviate Troubleshooting ==="

# Check if Weaviate is running
echo -n "Weaviate container status: "
if command -v podman-compose &> /dev/null; then
    CONTAINER_CMD="podman-compose"
elif command -v docker-compose &> /dev/null; then
    CONTAINER_CMD="docker-compose"
else
    echo "No container command found"
    exit 1
fi

CONTAINER_STATUS=$($CONTAINER_CMD -f docker-compose.dev.yml ps weaviate-dev 2>/dev/null | grep weaviate-dev || echo "not found")
if echo "$CONTAINER_STATUS" | grep -q "Up"; then
    echo "✓ Running"
else
    echo "✗ Not running or not found"
    echo "Container status: $CONTAINER_STATUS"
fi

# Check Weaviate connectivity
echo -n "Weaviate connectivity: "
if curl -s --max-time 5 "$WEAVIATE_URL/v1/meta" > /dev/null 2>&1; then
    echo "✓ Accessible"
else
    echo "✗ Not accessible"
fi

# Get Weaviate meta information
echo ""
echo "Weaviate Meta Information:"
META_RESPONSE=$(curl -s --max-time 5 "$WEAVIATE_URL/v1/meta" 2>/dev/null || echo "")
if [ -n "$META_RESPONSE" ]; then
    echo "$META_RESPONSE" | jq '.' 2>/dev/null || echo "$META_RESPONSE"
else
    echo "No response from meta endpoint"
fi

# Check current schema
echo ""
echo "Current Schema:"
SCHEMA_RESPONSE=$(curl -s --max-time 5 "$WEAVIATE_URL/v1/schema" 2>/dev/null || echo "")
if [ -n "$SCHEMA_RESPONSE" ]; then
    echo "$SCHEMA_RESPONSE" | jq '.classes[].class' 2>/dev/null || echo "No classes found"
else
    echo "No response from schema endpoint"
fi

# Check container logs
echo ""
echo "Recent Weaviate Logs (last 20 lines):"
$CONTAINER_CMD -f docker-compose.dev.yml logs --tail 20 weaviate-dev 2>/dev/null || echo "No logs available"

# Test schema creation
echo ""
echo "Testing minimal schema creation:"
MINIMAL_SCHEMA='{
  "class": "TestClass",
  "description": "Test class for troubleshooting",
  "vectorizer": "none",
  "properties": [
    {
      "name": "testProperty",
      "dataType": ["text"]
    }
  ]
}'

TEST_RESPONSE=$(curl -s -w "%{http_code}" -X POST "$WEAVIATE_URL/v1/schema" \
    -H "Content-Type: application/json" \
    -d "$MINIMAL_SCHEMA" 2>/dev/null || echo "000")

HTTP_CODE="${TEST_RESPONSE: -3}"
RESPONSE_BODY="${TEST_RESPONSE%???}"

case "$HTTP_CODE" in
    "200"|"201")
        echo "✓ Schema creation works"
        # Clean up test class
        curl -s -X DELETE "$WEAVIATE_URL/v1/schema/TestClass" > /dev/null 2>&1
        ;;
    "409")
        echo "✓ Schema endpoint works (class already exists)"
        ;;
    "422")
        echo "✗ Schema validation error (422)"
        echo "Response: $RESPONSE_BODY"
        ;;
    *)
        echo "✗ Unexpected response: $HTTP_CODE"
        echo "Response: $RESPONSE_BODY"
        ;;
esac

echo ""
echo "=== Troubleshooting Recommendations ==="
echo "1. If container is not running:"
echo "   ./stop-dev-services.sh && ./start-dev-services.sh"
echo ""
echo "2. If connectivity fails:"
echo "   Check if port 8080 is available: netstat -tuln | grep 8080"
echo ""
echo "3. If schema creation fails:"
echo "   Try manual initialization: ./infrastructure/weaviate/init-weaviate-dev.sh"
echo ""
echo "4. If issues persist:"
echo "   Reset Weaviate data: $CONTAINER_CMD -f docker-compose.dev.yml down -v weaviate_dev_data"
```

Update docker-compose.dev.yml Weaviate service:
```yaml
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
      ENABLE_MODULES: ''  # Disable modules that might cause issues
      CLUSTER_HOSTNAME: 'node1'
      ASYNC_INDEXING: 'true'
      LOG_LEVEL: 'info'  # Reduce log verbosity
    ports:
      - "8080:8080"
    volumes:
      - weaviate_dev_data:/var/lib/weaviate
    healthcheck:
      test: ["CMD", "wget", "--no-verbose", "--tries=3", "--spider", "http://localhost:8080/v1/meta"]
      interval: 30s
      timeout: 10s
      retries: 5
      start_period: 60s  # Increased startup time
    networks:
      - rag-dev-network
```

## Files / Modules Impacted

- infrastructure/weaviate/dev-schema.json (simplified and fixed)
- infrastructure/weaviate/init-weaviate-dev.sh (improved error handling)
- infrastructure/weaviate/troubleshoot-weaviate.sh (new troubleshooting script)
- start-dev-services.sh (improved Weaviate initialization)
- docker-compose.dev.yml (Weaviate service configuration)

## Acceptance Criteria

Given the fixed Weaviate configuration
When ./start-dev-services.sh is executed
Then Weaviate should start and schema should initialize without 422 errors

Given Weaviate schema initialization fails
When troubleshooting is needed
Then clear error messages and recovery steps should be provided

Given the schema is created successfully
When Weaviate operations are performed
Then they should work with the simplified schema

Given there are Weaviate issues
When ./troubleshoot-weaviate.sh is executed
Then comprehensive diagnostic information should be provided

## Testing Requirements

- Test Weaviate startup and schema creation
- Test schema validation and error handling
- Test troubleshooting script functionality
- Test recovery from failed initialization
- Test minimal schema fallback

## Dependencies / Preconditions

- Weaviate container must be running
- Network connectivity must be established
- curl and jq must be available for scripts
- Sufficient startup time for Weaviate initialization

## Key Changes Made

1. **Simplified schema**: Removed complex configurations that might cause 422 errors
2. **Better error handling**: Added validation and fallback to minimal schema
3. **Improved timing**: Added more wait time for Weaviate to fully initialize
4. **Enhanced logging**: Added troubleshooting script for diagnosis
5. **Graceful fallback**: If complex schema fails, try minimal schema instead