## Summary
Integrated automatic knowledge graph extraction into document processing so uploaded documents now generate Neo4j-backed knowledge graphs without blocking search readiness if graph creation fails.

## Changes
- Updated `backend/src/main/java/com/rag/app/usecases/ProcessDocument.java`
- Updated `backend/src/main/java/com/rag/app/config/KnowledgeGraphConfiguration.java`
- Updated `backend/src/test/java/com/rag/app/usecases/ProcessDocumentTest.java`

## Impact
Document processing now stores vectors and then attempts knowledge extraction/building for supported file types. Successful uploads create browsable knowledge graphs via the existing knowledge graph API, while extraction or Neo4j failures are logged and do not prevent documents from becoming `READY` for search.

## Verification
- `./gradlew :backend:compileJava`
- `./gradlew :backend:test --tests com.rag.app.usecases.ProcessDocumentTest`
- `./gradlew :backend:quarkusDev`
- `curl http://localhost:8081/q/health`
- `curl -F "userId=11111111-1111-1111-1111-111111111111" -F "file=@/tmp/0086-knowledge.txt;type=text/plain" http://localhost:8081/api/documents/upload`
- `curl -H "X-User-ID: 22222222-2222-2222-2222-222222222222" http://localhost:8081/api/knowledge-graph/graphs`
- `curl -H "X-User-ID: 22222222-2222-2222-2222-222222222222" http://localhost:8081/api/knowledge-graph/graphs/<graphId>`
- `./gradlew :backend:test`
- `./gradlew build`

## Follow-ups
- Persist document-level knowledge processing state if the UI needs to distinguish successful graph extraction from graceful degradation cases.
