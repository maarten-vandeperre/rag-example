## Summary
Resolved the module build break by updating the chat-system facade test to match the current `QueryDocuments` and `ChatSystemFacadeImpl` dependency graph.

## Changes
- Updated `backend/chat-system/src/test/java/com/rag/app/chat/ChatSystemFacadeTest.java`
- Verified existing `backend/shared-kernel/src/main/java/com/rag/app/shared/domain/knowledge/valueobjects/GraphId.java`
- Verified existing Gradle module wiring in `backend/document-management/build.gradle`, `backend/build.gradle`, and `settings.gradle`

## Impact
The multi-module Gradle build now completes successfully, confirming current module dependencies and constructor wiring are consistent across shared-kernel, document-management, chat-system, and backend modules.

## Verification
- `./gradlew :backend:shared-kernel:compileJava`
- `./gradlew :backend:document-management:compileJava`
- `./gradlew :backend:chat-system:compileTestJava`
- `./gradlew :backend:chat-system:test`
- `./gradlew build`

## Follow-ups
- Consider adding dedicated constructor-compatibility tests when use case dependencies change to catch outdated module test wiring earlier.
