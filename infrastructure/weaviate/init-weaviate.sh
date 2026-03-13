#!/bin/sh
set -eu

WEAVIATE_URL="${WEAVIATE_URL:-http://weaviate:8080}"
SCHEMA_FILE="${SCHEMA_FILE:-/schema/weaviate-schema.json}"

until curl -fsS "${WEAVIATE_URL}/v1/.well-known/ready" >/dev/null; do
  sleep 2
done

CLASS_NAME=$(sed -n 's/.*"class"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' "${SCHEMA_FILE}" | head -n 1)

if [ -z "${CLASS_NAME}" ]; then
  echo "Unable to determine schema class name"
  exit 1
fi

HTTP_CODE=$(curl -sS -o /tmp/weaviate-schema-response.json -w "%{http_code}" \
  -X POST "${WEAVIATE_URL}/v1/schema" \
  -H "Content-Type: application/json" \
  --data @"${SCHEMA_FILE}")

if [ "${HTTP_CODE}" = "200" ]; then
  echo "Weaviate schema initialized successfully"
  exit 0
fi

if [ "${HTTP_CODE}" = "422" ] && grep -q "already exists" /tmp/weaviate-schema-response.json; then
  echo "Weaviate schema already exists"
  exit 0
fi

echo "Weaviate schema initialization failed"
cat /tmp/weaviate-schema-response.json
exit 1
