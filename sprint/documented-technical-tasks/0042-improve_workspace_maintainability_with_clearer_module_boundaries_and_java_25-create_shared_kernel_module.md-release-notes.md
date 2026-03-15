## Summary
Created an isolated `backend/shared-kernel` Gradle module with common value objects, domain events, exceptions, interfaces, and validation utilities for cross-module use.

## Changes
Updated `settings.gradle` to include `:backend:shared-kernel`.
Added `backend/shared-kernel/build.gradle` with Java library setup and shared-kernel boundary enforcement.
Added shared exceptions, value objects, domain events, interfaces, and utility classes under `backend/shared-kernel/src/main/java/com/rag/app/shared/`.
Added shared-kernel validation and contract tests under `backend/shared-kernel/src/test/java/com/rag/app/shared/`.

## Impact
The workspace now has a dedicated place for reusable domain primitives and contracts, giving later module integration work a stable foundation instead of duplicating low-level concepts across modules.

## Verification
Executed `./gradlew --no-daemon :backend:shared-kernel:test`.
Executed `./gradlew --no-daemon :backend:shared-kernel:build test`.

## Follow-ups
Migrate duplicated value objects and shared validation logic in the document, chat, and user modules to the shared kernel once compatibility adapters are in place.
