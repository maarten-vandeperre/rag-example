## Summary
Added a new `backend/application-integration` module that coordinates document, chat, and user modules through an orchestrator, event bus, controller layer, and module health coordination.

## Changes
Updated `settings.gradle` to include `:backend:application-integration`.
Added `backend/application-integration/build.gradle` with module dependencies on shared-kernel, document-management, chat-system, and user-management.
Added integration-layer API DTOs, controllers, event bus, cross-module events, orchestrator, coordinator, and configuration classes under `backend/application-integration/src/main/java/com/rag/app/integration/`.
Added orchestration and controller tests under `backend/application-integration/src/test/java/com/rag/app/integration/`.

## Impact
Cross-module coordination now has a dedicated integration layer instead of direct module-to-module orchestration, giving the workspace a clearer place for application flow, event propagation, and integration-facing APIs.

## Verification
Executed `./gradlew --no-daemon :backend:application-integration:test`.
Executed `./gradlew --no-daemon :backend:application-integration:build :backend:test`.

## Follow-ups
Replace the placeholder extracted-content handoff and plain controller classes with production wiring to real backend adapters and health endpoints once the legacy backend surfaces are migrated to the new module graph.
