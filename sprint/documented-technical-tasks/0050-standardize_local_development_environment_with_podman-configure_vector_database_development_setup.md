# Configure Vector Database Development Setup

## Related User Story

User Story: standardize_local_development_environment_with_podman

## Objective

Configure Weaviate vector database specifically for local development with proper schema initialization, sample vector data, and development-specific configurations for semantic search functionality.

## Scope

- Create Weaviate schema configuration for development
- Set up document chunk vector storage
- Configure semantic search capabilities
- Add sample vector data for testing
- Create vector database management scripts

## Out of Scope

- Production vector database optimization
- Advanced vector indexing strategies
- Vector database clustering
- Performance tuning for large datasets

## Clean Architecture Placement

infrastructure

## Execution Dependencies

- 0047-standardize_local_development_environment_with_podman-create_development_services_compose.md

## Implementation Details

Create Weaviate schema configuration (infrastructure/weaviate/dev-schema.json):
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
        "pq": {
          "enabled": false,
          "bitCompression": false,
          "segments": 0,
          "centroids": 256,
          "encoder": {
            "type": "kmeans",
            "distribution": "log-normal"
          }
        },
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
          "dataType": ["string"],
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
          "dataType": ["string"],
          "description": "User ID who uploaded the document",
          "indexFilterable": true,
          "indexSearchable": false
        },
        {
          "name": "fileName",
          "dataType": ["string"],
          "description": "Original filename of the document",
          "indexFilterable": true,
          "indexSearchable": true
        },
        {
          "name": "fileType",
          "dataType": ["string"],
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
    },
    {
      "class": "UserQuery",
      "description": "User queries for analytics and improvement",
      "vectorizer": "none",
      "vectorIndexType": "hnsw",
      "vectorIndexConfig": {
        "distance": "cosine"
      },
      "properties": [
        {
          "name": "userId",
          "dataType": ["string"],
          "description": "ID of the user who made the query",
          "indexFilterable": true,
          "indexSearchable": false
        },
        {
          "name": "queryText",
          "dataType": ["text"],
          "description": "The actual query text",
          "indexFilterable": false,
          "indexSearchable": true,
          "tokenization": "word"
        },
        {
          "name": "timestamp",
          "dataType": ["date"],
          "description": "When the query was made",
          "indexFilterable": true,
          "indexSearchable": false
        },
        {
          "name": "responseTime",
          "dataType": ["int"],
          "description": "Response time in milliseconds",
          "indexFilterable": true,
          "indexSearchable": false
        },
        {
          "name": "foundResults",
          "dataType": ["boolean"],
          "description": "Whether relevant results were found",
          "indexFilterable": true,
          "indexSearchable": false
        }
      ]
    }
  ]
}
```

Create Weaviate initialization script (infrastructure/weaviate/init-weaviate-dev.sh):
```bash
#!/bin/bash
set -e

WEAVIATE_URL="http://localhost:8080"
SCHEMA_FILE="dev-schema.json"

echo "=== Initializing Weaviate Development Setup ==="

# Wait for Weaviate to be ready
echo "Waiting for Weaviate to be ready..."
until curl -f "${WEAVIATE_URL}/v1/meta" > /dev/null 2>&1; do
    echo -n "."
    sleep 2
done
echo " ✓"

# Check if schema already exists
echo "Checking existing schema..."
EXISTING_CLASSES=$(curl -s "${WEAVIATE_URL}/v1/schema" | jq -r '.classes[]?.class // empty' 2>/dev/null || echo "")

if echo "$EXISTING_CLASSES" | grep -q "DocumentChunk"; then
    echo "DocumentChunk class already exists, skipping schema creation"
else
    echo "Creating Weaviate schema..."
    
    # Create schema
    curl -s -X POST "${WEAVIATE_URL}/v1/schema" \
        -H "Content-Type: application/json" \
        -d @"${SCHEMA_FILE}"
    
    if [ $? -eq 0 ]; then
        echo "✓ Schema created successfully"
    else
        echo "✗ Failed to create schema"
        exit 1
    fi
fi

# Verify schema
echo "Verifying schema..."
SCHEMA_RESPONSE=$(curl -s "${WEAVIATE_URL}/v1/schema")
DOCUMENT_CHUNK_EXISTS=$(echo "$SCHEMA_RESPONSE" | jq -r '.classes[] | select(.class == "DocumentChunk") | .class' 2>/dev/null || echo "")

if [ "$DOCUMENT_CHUNK_EXISTS" = "DocumentChunk" ]; then
    echo "✓ DocumentChunk class verified"
else
    echo "✗ DocumentChunk class not found"
    exit 1
fi

echo ""
echo "=== Weaviate Development Setup Complete ==="
echo "Weaviate URL: ${WEAVIATE_URL}"
echo "GraphQL Endpoint: ${WEAVIATE_URL}/v1/graphql"
echo "REST API: ${WEAVIATE_URL}/v1"
echo ""
echo "Available classes:"
curl -s "${WEAVIATE_URL}/v1/schema" | jq -r '.classes[].class'
```

Create sample vector data script (infrastructure/weaviate/load-sample-data.sh):
```bash
#!/bin/bash
set -e

WEAVIATE_URL="http://localhost:8080"

echo "=== Loading Sample Vector Data ==="

# Sample document chunks with mock embeddings
# In a real scenario, these would be generated by an embedding model

# Sample chunk 1
echo "Loading sample chunk 1..."
curl -s -X POST "${WEAVIATE_URL}/v1/objects" \
    -H "Content-Type: application/json" \
    -d '{
        "class": "DocumentChunk",
        "properties": {
            "documentId": "doc-001",
            "chunkIndex": 1,
            "textContent": "This is the introduction to the sample guide. It covers the basics of document management and provides step-by-step instructions for organizing files.",
            "uploadedBy": "user-001",
            "fileName": "sample-guide.pdf",
            "fileType": "PDF",
            "createdAt": "2024-01-15T10:00:00Z",
            "chunkSize": 150
        },
        "vector": [0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0]
    }'

# Sample chunk 2
echo "Loading sample chunk 2..."
curl -s -X POST "${WEAVIATE_URL}/v1/objects" \
    -H "Content-Type: application/json" \
    -d '{
        "class": "DocumentChunk",
        "properties": {
            "documentId": "doc-001",
            "chunkIndex": 2,
            "textContent": "Chapter 2 explains how to upload files. Click the upload button and select your file. The system supports PDF, Markdown, and plain text files up to 40MB.",
            "uploadedBy": "user-001",
            "fileName": "sample-guide.pdf",
            "fileType": "PDF",
            "createdAt": "2024-01-15T10:00:00Z",
            "chunkSize": 160
        },
        "vector": [0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0, 0.1]
    }'

# Sample chunk 3
echo "Loading sample chunk 3..."
curl -s -X POST "${WEAVIATE_URL}/v1/objects" \
    -H "Content-Type: application/json" \
    -d '{
        "class": "DocumentChunk",
        "properties": {
            "documentId": "doc-002",
            "chunkIndex": 1,
            "textContent": "Project README: This project is a RAG application for document management and chat. It allows users to upload documents and ask questions about their content.",
            "uploadedBy": "user-001",
            "fileName": "project-readme.md",
            "fileType": "MARKDOWN",
            "createdAt": "2024-01-15T11:00:00Z",
            "chunkSize": 170
        },
        "vector": [0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0, 0.1, 0.2]
    }'

# Sample chunk 4
echo "Loading sample chunk 4..."
curl -s -X POST "${WEAVIATE_URL}/v1/objects" \
    -H "Content-Type: application/json" \
    -d '{
        "class": "DocumentChunk",
        "properties": {
            "documentId": "doc-003",
            "chunkIndex": 1,
            "textContent": "Meeting Notes: Quarterly review discussion. Key points: budget approval, timeline updates, and resource allocation for the next quarter.",
            "uploadedBy": "user-003",
            "fileName": "meeting-notes.txt",
            "fileType": "PLAIN_TEXT",
            "createdAt": "2024-01-15T12:00:00Z",
            "chunkSize": 140
        },
        "vector": [0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0, 0.1, 0.2, 0.3]
    }'

echo "✓ Sample vector data loaded"

# Verify data
echo ""
echo "Verifying loaded data..."
CHUNK_COUNT=$(curl -s "${WEAVIATE_URL}/v1/objects?class=DocumentChunk" | jq '.objects | length')
echo "Total DocumentChunk objects: $CHUNK_COUNT"
```

Create vector database testing script (infrastructure/weaviate/test-vector-search.sh):
```bash
#!/bin/bash
set -e

WEAVIATE_URL="http://localhost:8080"

echo "=== Testing Vector Search Functionality ==="

# Test 1: Basic object retrieval
echo "Test 1: Retrieving all DocumentChunk objects..."
OBJECTS=$(curl -s "${WEAVIATE_URL}/v1/objects?class=DocumentChunk")
OBJECT_COUNT=$(echo "$OBJECTS" | jq '.objects | length')
echo "Found $OBJECT_COUNT DocumentChunk objects"

# Test 2: GraphQL query
echo ""
echo "Test 2: GraphQL query for document chunks..."
curl -s -X POST "${WEAVIATE_URL}/v1/graphql" \
    -H "Content-Type: application/json" \
    -d '{
        "query": "{ Get { DocumentChunk(limit: 3) { documentId fileName textContent chunkIndex } } }"
    }' | jq '.data.Get.DocumentChunk'

# Test 3: Filtered search
echo ""
echo "Test 3: Filtered search by user..."
curl -s -X POST "${WEAVIATE_URL}/v1/graphql" \
    -H "Content-Type: application/json" \
    -d '{
        "query": "{ Get { DocumentChunk(where: {path: [\"uploadedBy\"], operator: Equal, valueString: \"user-001\"}) { documentId fileName textContent uploadedBy } } }"
    }' | jq '.data.Get.DocumentChunk'

# Test 4: Vector similarity search (with mock query vector)
echo ""
echo "Test 4: Vector similarity search..."
curl -s -X POST "${WEAVIATE_URL}/v1/graphql" \
    -H "Content-Type: application/json" \
    -d '{
        "query": "{ Get { DocumentChunk(nearVector: {vector: [0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0]}, limit: 2) { documentId fileName textContent _additional { distance } } } }"
    }' | jq '.data.Get.DocumentChunk'

echo ""
echo "✓ Vector search tests completed"
```

Create vector database management script (infrastructure/weaviate/manage-weaviate-dev.sh):
```bash
#!/bin/bash

WEAVIATE_URL="http://localhost:8080"

show_help() {
    echo "Weaviate Development Management Script"
    echo ""
    echo "Usage: $0 [command]"
    echo ""
    echo "Commands:"
    echo "  status      - Show Weaviate status and statistics"
    echo "  schema      - Display current schema"
    echo "  reset       - Reset all data (WARNING: destructive)"
    echo "  backup      - Create a backup of current data"
    echo "  load-sample - Load sample development data"
    echo "  test        - Run basic functionality tests"
    echo "  help        - Show this help message"
}

show_status() {
    echo "=== Weaviate Status ==="
    
    if curl -f "${WEAVIATE_URL}/v1/meta" > /dev/null 2>&1; then
        echo "✓ Weaviate is running"
        
        # Get meta information
        echo ""
        echo "Meta Information:"
        curl -s "${WEAVIATE_URL}/v1/meta" | jq '{hostname, version}'
        
        # Get object counts
        echo ""
        echo "Object Counts:"
        CHUNK_COUNT=$(curl -s "${WEAVIATE_URL}/v1/objects?class=DocumentChunk" | jq '.objects | length' 2>/dev/null || echo "0")
        QUERY_COUNT=$(curl -s "${WEAVIATE_URL}/v1/objects?class=UserQuery" | jq '.objects | length' 2>/dev/null || echo "0")
        
        echo "DocumentChunk objects: $CHUNK_COUNT"
        echo "UserQuery objects: $QUERY_COUNT"
        
    else
        echo "✗ Weaviate is not accessible"
        echo "Please start the development services: ./start-dev-services.sh"
    fi
}

show_schema() {
    echo "=== Weaviate Schema ==="
    curl -s "${WEAVIATE_URL}/v1/schema" | jq '.classes[] | {class: .class, description: .description, properties: [.properties[] | {name: .name, dataType: .dataType}]}'
}

reset_data() {
    echo "=== Resetting Weaviate Data ==="
    echo "WARNING: This will delete all data in Weaviate!"
    read -p "Are you sure? (y/N): " -n 1 -r
    echo
    
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        # Delete all objects
        echo "Deleting all DocumentChunk objects..."
        curl -s -X DELETE "${WEAVIATE_URL}/v1/schema/DocumentChunk"
        
        echo "Deleting all UserQuery objects..."
        curl -s -X DELETE "${WEAVIATE_URL}/v1/schema/UserQuery"
        
        # Recreate schema
        echo "Recreating schema..."
        ./init-weaviate-dev.sh
        
        echo "✓ Weaviate data reset complete"
    else
        echo "Reset cancelled"
    fi
}

backup_data() {
    echo "=== Backing up Weaviate Data ==="
    
    BACKUP_DIR="./backups"
    TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
    BACKUP_FILE="${BACKUP_DIR}/weaviate_backup_${TIMESTAMP}.json"
    
    mkdir -p "$BACKUP_DIR"
    
    # Export all objects
    curl -s "${WEAVIATE_URL}/v1/objects" > "$BACKUP_FILE"
    
    echo "✓ Backup created: $BACKUP_FILE"
    echo "File size: $(du -h "$BACKUP_FILE" | cut -f1)"
}

case "$1" in
    status)
        show_status
        ;;
    schema)
        show_schema
        ;;
    reset)
        reset_data
        ;;
    backup)
        backup_data
        ;;
    load-sample)
        ./load-sample-data.sh
        ;;
    test)
        ./test-vector-search.sh
        ;;
    help|*)
        show_help
        ;;
esac
```

Create Weaviate configuration for application (backend/src/main/resources/application-dev.properties):
```properties
# Weaviate Configuration for Development
app.vectorstore.provider=weaviate
app.vectorstore.url=http://localhost:8080
app.vectorstore.api-key=
app.vectorstore.timeout=30000

# Vector Configuration
app.vector.dimension=384
app.vector.similarity-threshold=0.7
app.vector.max-results=10

# Embedding Configuration (for development)
app.embedding.provider=sentence-transformers
app.embedding.model=all-MiniLM-L6-v2
app.embedding.batch-size=32
```

## Files / Modules Impacted

- infrastructure/weaviate/dev-schema.json
- infrastructure/weaviate/init-weaviate-dev.sh
- infrastructure/weaviate/load-sample-data.sh
- infrastructure/weaviate/test-vector-search.sh
- infrastructure/weaviate/manage-weaviate-dev.sh
- backend/src/main/resources/application-dev.properties
- docker-compose.dev.yml (volume mounts for scripts)

## Acceptance Criteria

Given Weaviate is running in development
When the schema initialization script is executed
Then DocumentChunk and UserQuery classes should be created

Given sample vector data is loaded
When vector search queries are performed
Then relevant document chunks should be returned

Given the vector database is configured
When the application performs semantic search
Then it should be able to store and retrieve document vectors

Given management scripts are provided
When developers need to manage vector data
Then they should be able to use the provided tools

## Testing Requirements

- Test Weaviate schema creation and validation
- Test vector data storage and retrieval
- Test semantic search functionality
- Test GraphQL and REST API access
- Test data backup and reset functionality

## Dependencies / Preconditions

- Weaviate container must be running
- Network connectivity must be established
- curl and jq must be available for scripts
- Sufficient memory for vector operations