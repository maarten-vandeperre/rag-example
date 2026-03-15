#!/bin/bash
set -euo pipefail

FRONTEND_URL="${FRONTEND_URL:-http://localhost:3000}"
BACKEND_URL="${BACKEND_URL:-http://localhost:8081}"
KEYCLOAK_URL="${KEYCLOAK_URL:-http://localhost:8180}"

printf '=== Testing Frontend Development Integration ===\n'
curl -fsS "$FRONTEND_URL" >/dev/null
printf 'Frontend is accessible\n'

if curl -fsS "$BACKEND_URL/q/health" >/dev/null 2>&1; then
  printf 'Backend is accessible\n'
else
  printf 'Backend is not accessible\n'
fi

if curl -fsS "$KEYCLOAK_URL/health/ready" >/dev/null 2>&1; then
  printf 'Keycloak is accessible\n'
else
  printf 'Keycloak is not accessible\n'
fi

headers=$(curl -sSI -H 'Origin: http://localhost:3000' -H 'Access-Control-Request-Method: GET' -H 'Access-Control-Request-Headers: Content-Type,Authorization' -X OPTIONS "$BACKEND_URL/api/documents")
if printf '%s' "$headers" | grep -iq 'access-control-allow-origin'; then
  printf 'CORS configured correctly\n'
else
  printf 'CORS not configured properly\n'
  exit 1
fi

curl -fsS "$BACKEND_URL/q/openapi" >/dev/null
realm=$(curl -fsS "$KEYCLOAK_URL/realms/rag-app-dev" 2>/dev/null || true)
printf '%s' "$realm" | grep -q 'rag-app-dev'
printf 'Frontend development integration test complete\n'
