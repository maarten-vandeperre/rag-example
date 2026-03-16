## Summary
Resolved the chat timeout issue by increasing the default chat response budget to 30 seconds and separating the frontend HTTP timeout from the backend processing limit so the browser no longer aborts requests before the API can return a result.

## Changes
- Updated `backend/src/main/java/com/rag/app/api/dto/ChatQueryRequest.java`
- Updated `frontend/src/services/ChatApiClient.js`
- Updated `frontend/src/components/ChatWorkspace/ChatWorkspace.jsx`
- Updated `frontend/src/services/ChatApiClient.test.js`

## Impact
Chat requests now allow a 30 second server-side processing window and a client-side timeout buffer, which prevents premature frontend aborts while still preserving bounded request handling and user-facing timeout behavior.

## Verification
- `./gradlew :backend:compileJava`
- `./gradlew :backend:test --tests com.rag.app.api.ChatControllerTest`
- `CI=true npm test -- --runInBand --runTestsByPath "src/services/ChatApiClient.test.js" "src/components/ChatWorkspace/ChatWorkspace.test.jsx"`
- `CI=true npm run build`
- `./start-dev-services.sh`
- `./gradlew :backend:quarkusDev`
- `curl http://localhost:8081/q/health`
- `curl -F "userId=11111111-1111-1111-1111-111111111111" -F "file=@/tmp/0090-chat.txt;type=text/plain" http://localhost:8081/api/documents/upload`
- `curl -X POST -H "Content-Type: application/json" -H "X-User-Id: 11111111-1111-1111-1111-111111111111" -d '{"question":"What does this timeout verification document say?","maxResponseTimeMs":30000}' http://localhost:8081/api/chat/query`
- `./gradlew :backend:compileJava :backend:test build`

## Follow-ups
- Consider surfacing the configured chat timeout budget in the UI so users understand how long long-running knowledge-base queries may take before failing.
