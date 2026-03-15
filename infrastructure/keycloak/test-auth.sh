#!/bin/bash
set -euo pipefail

KEYCLOAK_URL="${KEYCLOAK_URL:-http://localhost:8180}"
REALM="${KEYCLOAK_REALM:-rag-app-dev}"

if ! command -v curl >/dev/null 2>&1; then
  printf 'ERROR: curl is required.\n'
  exit 1
fi

if ! command -v jq >/dev/null 2>&1; then
  printf 'ERROR: jq is required.\n'
  exit 1
fi

if ! curl -fsS "${KEYCLOAK_URL}/health/ready" >/dev/null 2>&1; then
  printf 'ERROR: Keycloak is not ready.\n'
  printf 'Please ensure Keycloak is running: ./start-dev-services.sh\n'
  exit 1
fi

REALM_CHECK=$(curl -s "${KEYCLOAK_URL}/realms/${REALM}" || true)
if ! printf '%s' "$REALM_CHECK" | grep -q "$REALM"; then
  printf 'ERROR: Realm %s not found.\n' "$REALM"
  printf 'Please configure the realm: ./infrastructure/keycloak/configure-dev-realm.sh\n'
  exit 1
fi

request_token() {
  local username="$1"
  local password="$2"
  curl -fsS -X POST "${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/token" \
    -H 'Content-Type: application/x-www-form-urlencoded' \
    -d "username=${username}" \
    -d "password=${password}" \
    -d 'grant_type=password' \
    -d 'client_id=rag-app-frontend' | jq -r '.access_token'
}

printf '=== Testing Keycloak Authentication ===\n'

STANDARD_TOKEN=$(request_token 'john.doe' 'password123')
ADMIN_TOKEN=$(request_token 'jane.admin' 'admin123')
TEST_TOKEN=$(request_token 'test.user' 'test123')
BACKEND_TOKEN=$(curl -fsS -X POST "${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/token" \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'grant_type=client_credentials' \
  -d 'client_id=rag-app-backend' \
  -d 'client_secret=backend-dev-secret' | jq -r '.access_token')

for token_name in STANDARD_TOKEN ADMIN_TOKEN TEST_TOKEN BACKEND_TOKEN; do
  token_value=$(eval printf '%s' "\$$token_name")
  if [ -z "$token_value" ] || [ "$token_value" = "null" ]; then
    printf 'ERROR: %s not issued.\n' "$token_name"
    exit 1
  fi
done

printf 'john.doe token issued\n'
printf 'jane.admin token issued\n'
printf 'test.user token issued\n'
printf 'backend client token issued\n'

printf 'All authentication tests passed\n'
