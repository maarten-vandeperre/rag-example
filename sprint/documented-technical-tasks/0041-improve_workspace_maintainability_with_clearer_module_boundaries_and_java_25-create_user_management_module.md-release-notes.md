## Summary
Created an isolated `backend/user-management` Gradle module with user domain types, authentication and authorization use cases, facade wiring, session/auth adapters, and module-boundary verification.

## Changes
Updated `settings.gradle` to include `:backend:user-management`.
Added `backend/user-management/build.gradle` with Java library setup and module-boundary enforcement.
Added user-management domain, interfaces, use cases, facade, and infrastructure classes under `backend/user-management/src/main/java/com/rag/app/user/`.
Added user-management facade, authorization, and boundary tests under `backend/user-management/src/test/java/com/rag/app/user/`.

## Impact
User authentication, authorization, and role-management behavior now has a standalone backend module baseline that can evolve behind dedicated interfaces instead of remaining embedded in the legacy monolithic backend structure.

## Verification
Executed `./gradlew --no-daemon :backend:user-management:test`.
Executed `./gradlew --no-daemon :backend:user-management:build test`.

## Follow-ups
Replace placeholder JDBC wiring and simple password/session behavior with production-grade persistence and authentication integrations, then route existing backend user flows through the new facade.
