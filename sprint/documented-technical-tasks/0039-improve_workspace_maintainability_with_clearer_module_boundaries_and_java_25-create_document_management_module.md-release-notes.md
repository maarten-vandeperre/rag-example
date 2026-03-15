## Summary
Created an isolated `backend/document-management` Gradle module with its own document domain, use cases, facade, storage contracts, and module-level verification.

## Changes
Updated `settings.gradle` to include `:backend:document-management`.
Added `backend/document-management/build.gradle` with Java library setup and module-boundary enforcement.
Added document-management domain, interfaces, use cases, facade, and infrastructure classes under `backend/document-management/src/main/java/com/rag/app/document/`.
Added facade lifecycle and boundary tests under `backend/document-management/src/test/java/com/rag/app/document/`.

## Impact
Document workflows now have a standalone backend module baseline that can evolve independently from the legacy monolithic backend package layout.

## Verification
Executed `./gradlew --no-daemon :backend:document-management:test`.
Executed `./gradlew --no-daemon :backend:document-management:build test`.

## Follow-ups
Replace placeholder JDBC and extraction adapters with production wiring and begin redirecting existing backend document flows to the new facade.
