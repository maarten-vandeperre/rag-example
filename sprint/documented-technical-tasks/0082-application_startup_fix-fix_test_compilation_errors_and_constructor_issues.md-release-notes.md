## Summary
Aligned test fixtures with the current upload and chat constructor signatures so backend test compilation remains stable as dependencies evolve.

## Changes
- Updated `backend/src/test/java/com/rag/app/api/DocumentUploadControllerTest.java`
- Updated `backend/chat-system/src/test/java/com/rag/app/chat/ChatSystemFacadeTest.java`

## Impact
Backend tests now use centralized constructor setup for document uploads and a more complete in-memory answer source repository, reducing drift between test doubles and production wiring.

## Verification
- `./gradlew :backend:compileTestJava`
- `./gradlew :backend:chat-system:test`
- `./gradlew :backend:test`
- `./gradlew build`

## Follow-ups
- Consider extracting shared test builders for controller and facade wiring if more constructor dependencies are added across modules.
