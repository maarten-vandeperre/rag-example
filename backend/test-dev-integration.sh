#!/bin/bash
set -euo pipefail

BACKEND_URL="${BACKEND_URL:-http://localhost:8081}"

if ! command -v curl >/dev/null 2>&1 || ! command -v jq >/dev/null 2>&1; then
  printf 'ERROR: curl and jq are required.\n'
  exit 1
fi

printf '=== Testing Backend Development Integration ===\n'
printf 'Waiting for backend to be ready'
until curl -fsS "${BACKEND_URL}/q/health/ready" >/dev/null 2>&1; do
  printf '.'
  sleep 2
done
printf ' ready\n\n'

health_status=$(curl -fsS "${BACKEND_URL}/q/health" | jq -r '.status')
ready_status=$(curl -fsS "${BACKEND_URL}/q/health/ready" | jq -r '.status')
live_status=$(curl -fsS "${BACKEND_URL}/q/health/live" | jq -r '.status')
printf 'Health: %s\nReadiness: %s\nLiveness: %s\n' "$health_status" "$ready_status" "$live_status"

services_health=$(curl -fsS "${BACKEND_URL}/q/health" | jq -r '.checks[]? | select(.name == "development-services")')
if [ -n "$services_health" ]; then
  printf 'Database: %s\n' "$(printf '%s' "$services_health" | jq -r '.data.database')"
  printf 'Weaviate: %s\n' "$(printf '%s' "$services_health" | jq -r '.data.weaviate')"
  printf 'Keycloak: %s\n' "$(printf '%s' "$services_health" | jq -r '.data.keycloak')"
fi

curl -fsS "${BACKEND_URL}/q/swagger-ui" >/dev/null
curl -fsS "${BACKEND_URL}/q/openapi" >/dev/null

headers=$(curl -sSI -H 'Origin: http://localhost:3000' -H 'Access-Control-Request-Method: GET' -X OPTIONS "${BACKEND_URL}/api/health")
if printf '%s' "$headers" | grep -iq 'access-control-allow-origin'; then
  printf 'CORS configured correctly\n'
else
  printf 'ERROR: CORS headers not present\n'
  exit 1
fi

printf '=== Backend Development Integration Test Complete ===\n'
