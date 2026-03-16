## Summary
Verified backend startup against local development services and fixed two runtime issues discovered during live API checks: the document listing route shadowing and invalid non-UUID sample document/message identifiers.

## Changes
- Updated `backend/src/main/java/com/rag/app/api/DocumentLibraryResource.java`
- Added `backend/src/main/java/com/rag/app/api/AdminDocumentProgressResource.java`
- Updated `infrastructure/database/sample-dev-data.sql`

## Impact
The backend now starts successfully in dev mode, health checks report dependencies as up, document upload/content/admin endpoints respond correctly, and seeded development data no longer breaks document listing due to malformed identifiers.

## Verification
- `./gradlew :backend:compileJava`
- `./start-dev-services.sh`
- `./gradlew :backend:quarkusDev`
- `curl http://localhost:8081/q/health`
- `curl -F userId=11111111-1111-1111-1111-111111111111 -F file=@/tmp/0084-upload-verify.txt http://localhost:8081/api/documents/upload`
- `curl -H "X-User-Id: 11111111-1111-1111-1111-111111111111" http://localhost:8081/api/documents?includeAll=false`
- `curl -H "X-User-Id: 11111111-1111-1111-1111-111111111111" http://localhost:8081/api/documents/<documentId>/content`
- `curl -H "X-User-Id: 22222222-2222-2222-2222-222222222222" http://localhost:8081/api/admin/documents/progress`
- `curl http://localhost:8081/api/admin/documents/stuck`
- `curl -H "X-User-ID: 22222222-2222-2222-2222-222222222222" http://localhost:8081/api/knowledge-graph/graphs`
- `./gradlew :backend:test`
- `./gradlew build`

## Follow-ups
- Consider adding an HTTP-level regression test for `/api/documents` routing so path shadowing is caught before manual startup verification.
