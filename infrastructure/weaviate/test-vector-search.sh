#!/bin/bash
set -euo pipefail

WEAVIATE_URL="${WEAVIATE_URL:-http://localhost:8080}"

if ! command -v curl >/dev/null 2>&1 || ! command -v jq >/dev/null 2>&1; then
  printf 'ERROR: curl and jq are required.\n'
  exit 1
fi

printf '=== Testing Vector Search Functionality ===\n'
curl -fsS "${WEAVIATE_URL}/v1/objects?class=DocumentChunk" | jq '.objects | length'
curl -fsS -X POST "${WEAVIATE_URL}/v1/graphql" -H 'Content-Type: application/json' -d '{"query":"{ Get { DocumentChunk(limit: 2) { documentId fileName textContent chunkIndex } } }"}' | jq '.data.Get.DocumentChunk'
curl -fsS -X POST "${WEAVIATE_URL}/v1/graphql" -H 'Content-Type: application/json' -d '{"query":"{ Get { DocumentChunk(where: {path: [\"uploadedBy\"], operator: Equal, valueText: \"user-001\"}) { documentId fileName uploadedBy } } }"}' | jq '.data.Get.DocumentChunk'
curl -fsS -X POST "${WEAVIATE_URL}/v1/graphql" -H 'Content-Type: application/json' -d '{"query":"{ Get { DocumentChunk(nearVector: {vector: [0.1,0.2,0.3,0.4,0.5,0.6,0.7,0.8,0.9,1.0]}, limit: 2) { documentId fileName _additional { distance } } } }"}' | jq '.data.Get.DocumentChunk'

printf 'Vector search tests completed.\n'
