## Summary
Added a dedicated `backend/integration-tests` module with boundary, event, workflow, health, and backward-compatibility tests for the modular architecture.

## Changes
Updated `settings.gradle` to include `:backend:integration-tests`.
Added `backend/integration-tests/build.gradle` and `backend/integration-tests/src/test/resources/application-test.properties` for the new integration test module.
Added shared test fixtures in `backend/integration-tests/src/test/java/com/rag/app/integration/support/IntegrationTestFixtures.java`.
Added module boundary, module communication, event bus, end-to-end workflow, system health, and backward compatibility tests under `backend/integration-tests/src/test/java/com/rag/app/integration/`.

## Impact
The workspace now has a dedicated cross-module verification layer that exercises the integration architecture without relying only on individual module tests.

## Verification
Executed `./gradlew --no-daemon :backend:integration-tests:test`.
Executed `./gradlew --no-daemon :backend:integration-tests:build :backend:application-integration:test :backend:test`.

## Follow-ups
Expand these tests to hit real Quarkus HTTP endpoints and persistence-backed workflows once the integration layer is wired into production runtime components.
