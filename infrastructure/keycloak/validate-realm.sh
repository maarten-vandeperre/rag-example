#!/bin/bash
set -euo pipefail

REALM_FILE="${1:-$(dirname "$0")/dev-realm.json}"

printf '=== Validating Keycloak Realm Configuration ===\n'

if ! command -v jq >/dev/null 2>&1; then
  printf 'ERROR: jq is required.\n'
  exit 1
fi

if [ ! -f "$REALM_FILE" ]; then
  printf 'ERROR: Realm file not found: %s\n' "$REALM_FILE"
  exit 1
fi

printf 'Checking JSON syntax... '
jq empty "$REALM_FILE" >/dev/null
printf 'ok\n'

for field in postLogoutRedirectUris defaultRoles; do
  if jq -e --arg field "$field" '.clients[]? | has($field)' "$REALM_FILE" >/dev/null 2>&1; then
    printf 'WARNING: Found potentially incompatible client field: %s\n' "$field"
  fi
done

for field in realm enabled clients users; do
  if jq -e --arg field "$field" 'has($field)' "$REALM_FILE" >/dev/null 2>&1; then
    printf 'Required field present: %s\n' "$field"
  else
    printf 'ERROR: Missing required field: %s\n' "$field"
    exit 1
  fi
done

backend_client=$(jq -r '.clients[] | select(.clientId == "rag-app-backend") | .clientId' "$REALM_FILE")
frontend_client=$(jq -r '.clients[] | select(.clientId == "rag-app-frontend") | .clientId' "$REALM_FILE")
[ "$backend_client" = "rag-app-backend" ] || { printf 'ERROR: Backend client missing\n'; exit 1; }
[ "$frontend_client" = "rag-app-frontend" ] || { printf 'ERROR: Frontend client missing\n'; exit 1; }

printf 'Users configured:\n'
jq -r '.users[].username' "$REALM_FILE" | sed 's/^/  - /'

printf 'Realm configuration appears valid for Keycloak 23.0\n'
