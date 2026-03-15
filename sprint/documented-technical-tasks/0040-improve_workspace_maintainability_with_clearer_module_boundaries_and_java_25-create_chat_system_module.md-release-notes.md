## Summary
Created an isolated `backend/chat-system` Gradle module with chat domain types, query and history use cases, facade wiring, vector/search adapters, and module-boundary verification.

## Changes
Updated `settings.gradle` to include `:backend:chat-system`.
Added `backend/chat-system/build.gradle` with Java library setup and module-boundary enforcement.
Added chat-system domain, interfaces, use cases, facade, and infrastructure classes under `backend/chat-system/src/main/java/com/rag/app/chat/`.
Added chat-system facade, entity behavior, and boundary tests under `backend/chat-system/src/test/java/com/rag/app/chat/`.

## Impact
Chat and query behavior now has a standalone backend module baseline that can evolve behind dedicated interfaces instead of remaining embedded in the legacy monolithic backend structure.

## Verification
Executed `./gradlew --no-daemon :backend:chat-system:test`.
Executed `./gradlew --no-daemon :backend:chat-system:build test`.

## Follow-ups
Replace placeholder JDBC and vector/LLM adapters with production integrations and start routing the existing backend chat flow through the new facade.
