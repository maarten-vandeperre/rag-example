#!/bin/bash
set -euo pipefail

KEYCLOAK_URL="${KEYCLOAK_URL:-http://localhost:8180}"
ADMIN_USER="${KEYCLOAK_ADMIN:-admin}"
ADMIN_PASSWORD="${KEYCLOAK_ADMIN_PASSWORD:-admin123}"
REALM_NAME="${KEYCLOAK_REALM:-rag-app-dev}"
REALM_FILE="${1:-$(dirname "$0")/dev-realm.json}"

printf '=== Configuring Keycloak Development Realm ===\n'

if ! command -v curl >/dev/null 2>&1; then
  printf 'ERROR: curl is required.\n'
  exit 1
fi

if ! command -v jq >/dev/null 2>&1; then
  printf 'ERROR: jq is required.\n'
  exit 1
fi

if [ ! -f "$REALM_FILE" ]; then
  printf 'ERROR: Realm file not found: %s\n' "$REALM_FILE"
  exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
"${SCRIPT_DIR}/validate-realm.sh" "$REALM_FILE"

printf 'Waiting for Keycloak to be ready'
until curl -fsS "${KEYCLOAK_URL}/health/ready" >/dev/null 2>&1; do
  printf '.'
  sleep 5
done
printf ' ready\n'

printf 'Waiting for admin console to be ready...\n'
sleep 10

ADMIN_TOKEN=""
attempt=0
while [ "$attempt" -lt 10 ]; do
  ADMIN_TOKEN=$(curl -s -X POST "${KEYCLOAK_URL}/realms/master/protocol/openid-connect/token" \
    -H 'Content-Type: application/x-www-form-urlencoded' \
    -d "username=${ADMIN_USER}" \
    -d "password=${ADMIN_PASSWORD}" \
    -d 'grant_type=password' \
    -d 'client_id=admin-cli' | jq -r '.access_token // empty')
  if [ -n "$ADMIN_TOKEN" ] && [ "$ADMIN_TOKEN" != "null" ]; then
    break
  fi
  attempt=$((attempt + 1))
  printf 'Retrying admin token request... (attempt %s)\n' "$attempt"
  sleep 5
done

if [ -z "$ADMIN_TOKEN" ] || [ "$ADMIN_TOKEN" = "null" ]; then
  printf 'ERROR: Failed to obtain admin token.\n'
  if command -v podman-compose >/dev/null 2>&1; then
    podman-compose -f docker-compose.dev.yml logs --tail 20 keycloak-dev || true
  fi
  exit 1
fi

STATUS_CODE=$(curl -s -o /dev/null -w '%{http_code}' \
  -H "Authorization: Bearer ${ADMIN_TOKEN}" \
  "${KEYCLOAK_URL}/admin/realms/${REALM_NAME}")

if [ "$STATUS_CODE" = "200" ]; then
  printf 'Realm %s already exists\n' "$REALM_NAME"
else
  printf 'Realm %s not found; it should be imported during startup with --import-realm\n' "$REALM_NAME"
fi

REALM_INFO=$(curl -s -H "Authorization: Bearer ${ADMIN_TOKEN}" "${KEYCLOAK_URL}/admin/realms/${REALM_NAME}" || true)
if printf '%s' "$REALM_INFO" | grep -q "$REALM_NAME"; then
  printf 'Realm %s is accessible\n' "$REALM_NAME"
else
  printf 'WARNING: Realm verification failed; check Keycloak import logs.\n'
fi

printf '\nRealm URL: %s/realms/%s\n' "$KEYCLOAK_URL" "$REALM_NAME"
printf 'Admin Console: %s/admin/master/console/#/%s\n' "$KEYCLOAK_URL" "$REALM_NAME"
printf 'Users: john.doe/password123, jane.admin/admin123, test.user/test123\n'
printf 'Clients: rag-app-backend, rag-app-frontend\n'
