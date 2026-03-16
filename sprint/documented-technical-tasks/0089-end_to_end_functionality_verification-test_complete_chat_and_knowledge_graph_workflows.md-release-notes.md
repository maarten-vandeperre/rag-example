## Summary
Added automated end-to-end integration coverage for the combined upload, chat, and knowledge graph workflow, and verified the same journey live against the local development stack.

## Changes
- Updated `backend/src/main/java/com/rag/app/usecases/ProcessDocument.java`
- Updated `backend/src/main/java/com/rag/app/api/KnowledgeGraphResource.java`
- Updated `backend/src/test/java/integration/IntegrationTestSupport.java`
- Added `backend/src/test/java/integration/EndToEndWorkflowIntegrationTest.java`

## Impact
The repository now has explicit integration coverage for the full user journey from upload through chat answers and knowledge graph browsing, including forbidden admin access and missing-document chat scenarios. The knowledge graph API is also easier to instantiate in tests through constructor injection.

## Verification
- `./gradlew :backend:compileJava`
- `./gradlew :backend:compileTestJava`
- `./gradlew :backend:integrationTest --tests integration.EndToEndWorkflowIntegrationTest`
- `./start-dev-services.sh`
- `./gradlew :backend:quarkusDev`
- `curl http://localhost:8081/q/health`
- `curl -F "userId=11111111-1111-1111-1111-111111111111" -F "file=@/tmp/0089-workflow.txt;type=text/plain" http://localhost:8081/api/documents/upload`
- `curl -X POST -H "Content-Type: application/json" -H "X-User-Id: 11111111-1111-1111-1111-111111111111" -d '{"question":"How does semantic search retrieve document chunks?","maxResponseTimeMs":60000}' http://localhost:8081/api/chat/query`
- `curl -H "X-User-ID: 22222222-2222-2222-2222-222222222222" http://localhost:8081/api/knowledge-graph/graphs`
- `curl -H "X-User-ID: 22222222-2222-2222-2222-222222222222" http://localhost:8081/api/knowledge-graph/graphs/<graphId>`
- `curl -H "X-User-ID: 22222222-2222-2222-2222-222222222222" "http://localhost:8081/api/knowledge-graph/search?query=Ada&graphId=<graphId>"`
- `curl -H "X-User-ID: 22222222-2222-2222-2222-222222222222" "http://localhost:8081/api/knowledge-graph/statistics?graphId=<graphId>"`
- `./gradlew :backend:compileJava :backend:test :backend:integrationTest build`

## Follow-ups
- Consider adding a real container-backed integration profile for knowledge graph and chat workflows so live dev-service checks can be promoted into repeatable CI coverage.
