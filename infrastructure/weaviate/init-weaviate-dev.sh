#!/bin/bash
set -euo pipefail

WEAVIATE_URL="${WEAVIATE_URL:-http://localhost:8080}"
SCHEMA_FILE="${1:-$(dirname "$0")/dev-schema.json}"

if ! command -v curl >/dev/null 2>&1 || ! command -v jq >/dev/null 2>&1; then
  printf 'ERROR: curl and jq are required.\n'
  exit 1
fi

printf '=== Initializing Weaviate Development Setup ===\n'
printf 'Waiting for Weaviate to be ready'
until curl -fsS "${WEAVIATE_URL}/v1/meta" >/dev/null 2>&1; do
  printf '.'
  sleep 2
done
printf ' ready\n'

EXISTING_CLASSES=$(curl -fsS "${WEAVIATE_URL}/v1/schema" | jq -r '.classes[]?.class // empty')
if printf '%s\n' "$EXISTING_CLASSES" | grep -q '^DocumentChunk$'; then
  printf 'DocumentChunk class already exists, skipping creation.\n'
else
  curl -fsS -X POST "${WEAVIATE_URL}/v1/schema" -H 'Content-Type: application/json' --data-binary @"${SCHEMA_FILE}" >/dev/null
  printf 'Schema created successfully.\n'
fi

curl -fsS "${WEAVIATE_URL}/v1/schema" | jq -r '.classes[].class'
