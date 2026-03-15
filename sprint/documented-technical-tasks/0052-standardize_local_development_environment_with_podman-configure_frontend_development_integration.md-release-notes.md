## Summary
Configured the frontend for local development against the native backend and Podman services with dev environment variables, auth-aware API wiring, startup/test scripts, and a lightweight development tools panel.

## Changes
Added `frontend/.env.development`, `frontend/start-dev.sh`, and `frontend/test-dev-integration.sh` for local frontend development startup and smoke testing.
Updated `frontend/src/config/keycloak.js` and `frontend/src/services/ApiClient.js` to provide development authentication state, auth headers, user info, and backend health checks.
Added `frontend/src/components/DevTools/DevPanel.jsx` and `frontend/src/modules/shared/contexts/NotificationContext.jsx`, and updated `frontend/src/App.js` and `frontend/src/App.css` to initialize auth and surface dev tools.
Updated `frontend/package.json`, `frontend/build.gradle`, and `frontend/src/App.test.js` to support and verify the new development workflow.

## Impact
The frontend now has a clearer local development integration path for backend connectivity and Keycloak-aware behavior, while preserving the existing build and test workflow.

## Verification
Executed `chmod +x frontend/start-dev.sh frontend/test-dev-integration.sh`.
Executed `bash -n frontend/start-dev.sh frontend/test-dev-integration.sh`.
Executed `./gradlew --no-daemon :frontend:test :frontend:lint :frontend:buildProd`.
Executed `./gradlew --no-daemon :frontend:verify`.

## Follow-ups
Replace the current development Keycloak stub with the real browser client and connect the new dev integration test script to live backend and Keycloak instances once full auth flows are enabled.
