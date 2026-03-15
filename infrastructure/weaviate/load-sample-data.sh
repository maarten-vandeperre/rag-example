#!/bin/bash
set -euo pipefail

WEAVIATE_URL="${WEAVIATE_URL:-http://localhost:8080}"

if ! command -v curl >/dev/null 2>&1 || ! command -v jq >/dev/null 2>&1; then
  printf 'ERROR: curl and jq are required.\n'
  exit 1
fi

create_object() {
  local payload="$1"
  curl -fsS -X POST "${WEAVIATE_URL}/v1/objects" -H 'Content-Type: application/json' -d "$payload" >/dev/null
}

create_object '{"class":"DocumentChunk","properties":{"documentId":"doc-001","chunkIndex":1,"textContent":"This is the introduction to the sample guide. It covers the basics of document management.","uploadedBy":"user-001","fileName":"sample-guide.pdf","fileType":"PDF","createdAt":"2024-01-15T10:00:00Z","chunkSize":92},"vector":[0.1,0.2,0.3,0.4,0.5,0.6,0.7,0.8,0.9,1.0]}'
create_object '{"class":"DocumentChunk","properties":{"documentId":"doc-001","chunkIndex":2,"textContent":"Chapter 2 explains how to upload files. Click upload and select your file.","uploadedBy":"user-001","fileName":"sample-guide.pdf","fileType":"PDF","createdAt":"2024-01-15T10:02:00Z","chunkSize":78},"vector":[0.2,0.3,0.4,0.5,0.6,0.7,0.8,0.9,1.0,0.1]}'
create_object '{"class":"DocumentChunk","properties":{"documentId":"doc-002","chunkIndex":1,"textContent":"Project README for the RAG application with document chat support.","uploadedBy":"user-001","fileName":"project-readme.md","fileType":"MARKDOWN","createdAt":"2024-01-15T11:00:00Z","chunkSize":67},"vector":[0.3,0.4,0.5,0.6,0.7,0.8,0.9,1.0,0.1,0.2]}'
create_object '{"class":"UserQuery","properties":{"userId":"user-001","queryText":"How do I upload a document?","timestamp":"2024-01-15T12:00:00Z","responseTime":1200,"foundResults":true},"vector":[0.5,0.4,0.3,0.2,0.1,0.2,0.3,0.4,0.5,0.6]}'

printf 'Sample vector data loaded.\n'
curl -fsS "${WEAVIATE_URL}/v1/objects?class=DocumentChunk" | jq '.objects | length'
