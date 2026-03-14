## Summary
Implemented an in-memory vector store pipeline that chunks extracted document text, generates normalized embeddings, and serves relevance-ranked semantic search results.

## Changes
Updated `backend/src/main/java/com/rag/app/usecases/models/DocumentChunk.java`.
Updated `backend/src/main/java/com/rag/app/usecases/interfaces/VectorStore.java`.
Added `backend/src/main/java/com/rag/app/infrastructure/vector/TextChunker.java`.
Added `backend/src/main/java/com/rag/app/infrastructure/vector/EmbeddingGenerator.java`.
Added `backend/src/main/java/com/rag/app/infrastructure/vector/VectorStoreImpl.java`.
Added `backend/src/test/java/com/rag/app/infrastructure/vector/TextChunkerTest.java`.
Added `backend/src/test/java/com/rag/app/infrastructure/vector/EmbeddingGeneratorTest.java`.
Added `backend/src/test/java/com/rag/app/infrastructure/vector/VectorStoreImplTest.java`.

## Impact
Document processing can now persist searchable chunk embeddings in the application layer, and chat retrieval can reuse the same vector-backed storage contract for filtered semantic search.

## Verification
Ran `mvn -q -Dquarkus.platform.group-id=io.quarkus -Dquarkus.platform.artifact-id=quarkus-bom -Dquarkus.platform.version=2.16.5.Final -DskipTests compile`.
Ran `mvn -q -Dquarkus.platform.group-id=io.quarkus -Dquarkus.platform.artifact-id=quarkus-bom -Dquarkus.platform.version=2.16.5.Final test`.

## Follow-ups
Replace the in-memory chunk index with a persistent vector database adapter once the target store and deployment configuration are available.
