## Summary
Reorganized the frontend into product-area module folders with shared routing, layout, wrappers, hooks, services, and module-boundary linting support while preserving the existing UI behavior.

## Changes
Updated `frontend/src/App.js`, `frontend/src/App.css`, `frontend/src/App.test.js`, and added `frontend/src/routes/AppRoutes.js` for shared layout and lazy-loaded module routing.
Added modular structure under `frontend/src/modules/` for document management, chat system, user management, and shared frontend concerns.
Added custom module-boundary lint rule in `frontend/eslint-rules/no-cross-module-imports.js` and updated `frontend/package.json`, `frontend/package-lock.json`, `frontend/.eslintrc.json`, and `frontend/build.gradle` to run both standard and module-specific lint checks.
Added `react-router-dom` to support route-based module composition.

## Impact
The frontend now mirrors the backend module split more clearly, gives shared concerns a dedicated home, and provides a guardrail against direct cross-module imports.

## Verification
Executed `./gradlew --no-daemon :frontend:test :frontend:lint :frontend:buildProd`.
Executed `./gradlew --no-daemon :frontend:verify`.

## Follow-ups
Migrate the legacy `frontend/src/components/` and `frontend/src/services/` implementations fully into module-owned code so the wrapper layer can be removed in a later cleanup task.
