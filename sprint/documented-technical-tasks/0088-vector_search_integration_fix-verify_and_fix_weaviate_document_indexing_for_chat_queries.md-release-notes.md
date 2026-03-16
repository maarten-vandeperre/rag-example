## Summary
Implemented real Weaviate-backed document indexing and semantic retrieval for development so uploaded documents persist their chunks in Weaviate and remain searchable for chat queries after backend restarts.

## Changes
- Added `backend/src/main/java/com/rag/app/infrastructure/vector/WeaviateVectorStore.java`
- Updated `backend/src/main/java/com/rag/app/config/VectorStoreConfiguration.java`
- Updated `backend/src/main/resources/application-dev.properties`
- Updated `infrastructure/weaviate/init-weaviate-dev.sh`
- Added `backend/src/test/java/com/rag/app/infrastructure/vector/WeaviateVectorStoreTest.java`

## Impact
The dev backend now uses Weaviate instead of an in-memory fallback when `app.vectorstore.provider=weaviate`. Document chunks are stored with metadata, missing schema properties are added automatically during dev-service startup, and chat retrieval continues to work with a local fallback when Weaviate is temporarily unavailable during the current runtime.

## Verification
- `./gradlew :backend:compileJava`
- `./gradlew :backend:test --tests com.rag.app.infrastructure.vector.WeaviateVectorStoreTest`
- `./start-dev-services.sh`
- `./gradlew :backend:quarkusDev`
- `curl http://localhost:8081/q/health`
- `curl -F "userId=11111111-1111-1111-1111-111111111111" -F "file=@/tmp/0088-weaviate.txt;type=text/plain" http://localhost:8081/api/documents/upload`
- `curl http://localhost:8080/v1/graphql -H "Content-Type: application/json" -d '{"query":"{ Get { DocumentChunk(where: {path: [\"documentId\"], operator: Equal, valueText: \"<documentId>\"}) { documentId chunkIndex textContent fileName } } }"}'`
- `curl -X POST -H "Content-Type: application/json" -H "X-User-Id: 11111111-1111-1111-1111-111111111111" -d '{"question":"How does semantic search help chat questions?","maxResponseTimeMs":60000}' http://localhost:8081/api/chat/query`
- `./gradlew :backend:test`
- `./gradlew build`

## Follow-ups
- Consider adding a dedicated integration test that exercises the real Weaviate container through the backend instead of only a stubbed unit test plus manual dev verification.
