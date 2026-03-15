## Summary
Expanded the local Keycloak development setup with a richer realm definition, dedicated backend/frontend clients, development users and roles, helper scripts, and backend/frontend Keycloak config stubs.

## Changes
Updated `infrastructure/keycloak/dev-realm.json` with development roles, backend/frontend clients, user accounts, groups, and realm settings.
Added `infrastructure/keycloak/configure-dev-realm.sh` and `infrastructure/keycloak/test-auth.sh` for realm import/update and token-flow verification.
Added `backend/src/main/resources/application-dev.properties` for development OIDC settings.
Added `frontend/src/config/keycloak.js` and `frontend/public/silent-check-sso.html` for frontend Keycloak bootstrap wiring, and updated `.env.dev` with explicit Keycloak client settings.

## Impact
Local development now has a concrete Keycloak realm and supporting scripts that match the intended backend/frontend authentication split, making future auth integration tasks easier to wire end to end.

## Verification
Executed `chmod +x infrastructure/keycloak/configure-dev-realm.sh infrastructure/keycloak/test-auth.sh`.
Executed `bash -n infrastructure/keycloak/configure-dev-realm.sh infrastructure/keycloak/test-auth.sh`.
Executed `python3 -c 'import json; json.load(open("infrastructure/keycloak/dev-realm.json"))'`.
Executed `./gradlew --no-daemon healthCheck test`.

## Follow-ups
Replace the current frontend Keycloak placeholder initializer with the real `keycloak-js` client and connect backend OIDC configuration to actual secured endpoints in a follow-up integration task.
