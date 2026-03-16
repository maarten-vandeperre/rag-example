## Summary
Added a simple admin REST endpoint for viewing and cleaning up documents that remain stuck in the `UPLOADED` state.

## Changes
- Added `backend/src/main/java/com/rag/app/api/StuckDocumentResource.java`
- Added `backend/src/test/java/com/rag/app/api/StuckDocumentResourceTest.java`

## Impact
Administrators can now inspect stuck uploads and mark them as failed through a minimal JDBC-backed endpoint without introducing extra module dependencies.

## Verification
- `./gradlew :backend:compileJava`
- `./gradlew :backend:test --tests com.rag.app.api.StuckDocumentResourceTest`
- `./gradlew :backend:test`
- `./gradlew build`

## Follow-ups
- Add authorization checks if this endpoint is later exposed beyond trusted admin tooling.
