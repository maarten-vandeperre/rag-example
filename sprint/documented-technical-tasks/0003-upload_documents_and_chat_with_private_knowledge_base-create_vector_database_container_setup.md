# Create Vector Database container setup

## Related User Story

User Story: upload_documents_and_chat_with_private_knowledge_base

## Objective

Create vector database container setup using Weaviate for semantic search and document embedding storage with proper configuration for the RAG application.

## Scope

- Create Weaviate container configuration
- Define vector schema for document chunks
- Configure authentication and access control
- Set up persistent storage for vector data

## Out of Scope

- Vector database clustering
- Advanced vector indexing optimization
- Vector database backup strategies
- Production security hardening

## Clean Architecture Placement

infrastructure

## Execution Dependencies

- 0001-upload_documents_and_chat_with_private_knowledge_base-create_podman_compose_configuration.md

## Implementation Details

Create Weaviate setup with:

1. **Weaviate service configuration** in docker-compose.yml:
```yaml
weaviate:
  image: semitechnologies/weaviate:1.22.4
  ports:
    - "8080:8080"
  environment:
    QUERY_DEFAULTS_LIMIT: 25
    AUTHENTICATION_ANONYMOUS_ACCESS_ENABLED: 'true'
    PERSISTENCE_DATA_PATH: '/var/lib/weaviate'
    DEFAULT_VECTORIZER_MODULE: 'none'
    ENABLE_MODULES: 'text2vec-transformers'
    CLUSTER_HOSTNAME: 'node1'
  volumes:
    - weaviate-data:/var/lib/weaviate
```

2. **Vector schema initialization script** (weaviate-schema.json):
```json
{
  "class": "DocumentChunk",
  "description": "A chunk of text from an uploaded document",
  "vectorizer": "none",
  "properties": [
    {
      "name": "documentId",
      "dataType": ["string"],
      "description": "ID of the parent document"
    },
    {
      "name": "chunkIndex",
      "dataType": ["int"],
      "description": "Index of this chunk within the document"
    },
    {
      "name": "textContent",
      "dataType": ["text"],
      "description": "The actual text content of the chunk"
    },
    {
      "name": "uploadedBy",
      "dataType": ["string"],
      "description": "User ID who uploaded the document"
    },
    {
      "name": "fileName",
      "dataType": ["string"],
      "description": "Original filename of the document"
    },
    {
      "name": "createdAt",
      "dataType": ["date"],
      "description": "When this chunk was created"
    }
  ]
}
```

3. **Schema initialization script** (init-weaviate.sh):
```bash
#!/bin/bash
# Wait for Weaviate to be ready
until curl -f http://weaviate:8080/v1/meta; do
  echo "Waiting for Weaviate to be ready..."
  sleep 2
done

# Create the DocumentChunk class
curl -X POST \
  http://weaviate:8080/v1/schema \
  -H 'Content-Type: application/json' \
  -d @/schema/weaviate-schema.json

echo "Weaviate schema initialized successfully"
```

4. **Health check configuration**:
```yaml
healthcheck:
  test: ["CMD", "curl", "-f", "http://localhost:8080/v1/meta"]
  interval: 30s
  timeout: 10s
  retries: 3
  start_period: 40s
```

Environment variables:
- WEAVIATE_URL=http://weaviate:8080
- WEAVIATE_API_KEY= (empty for development)
- VECTOR_DIMENSION=384 (based on embedding model)

Volume configuration:
- Persistent storage for vector data
- Schema configuration files
- Backup location (if needed)

## Files / Modules Impacted

- docker-compose.yml (weaviate service)
- infrastructure/weaviate/weaviate-schema.json
- infrastructure/weaviate/init-weaviate.sh
- infrastructure/weaviate/docker-entrypoint.sh

## Acceptance Criteria

Given Weaviate container is started
When health check is performed
Then Weaviate should be accessible on port 8080

Given schema initialization script runs
When DocumentChunk class is created
Then vector operations should be possible

Given vector data is stored
When container is restarted
Then vector data should be preserved

Given semantic search is performed
When similar vectors are queried
Then relevant results should be returned

## Testing Requirements

- Test Weaviate container startup and health
- Test schema creation and validation
- Test vector storage and retrieval
- Test semantic search functionality
- Test data persistence across restarts

## Dependencies / Preconditions

- Podman Compose configuration must exist
- Weaviate image must be available
- Network connectivity between services must be configured