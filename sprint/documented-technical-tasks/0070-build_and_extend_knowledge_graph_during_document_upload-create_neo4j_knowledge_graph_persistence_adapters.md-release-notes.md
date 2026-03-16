## Summary
Added Neo4j-backed knowledge graph persistence adapters, mappers, query helpers, and configuration support for storing and querying shared-kernel knowledge graphs.

## Changes
- `backend/shared-kernel/build.gradle`
- `backend/shared-kernel/src/main/java/com/rag/app/shared/infrastructure/knowledge/KnowledgeGraphCypherQueries.java`
- `backend/shared-kernel/src/main/java/com/rag/app/shared/infrastructure/knowledge/Neo4jConfiguration.java`
- `backend/shared-kernel/src/main/java/com/rag/app/shared/infrastructure/knowledge/Neo4jKnowledgeGraphRepository.java`
- `backend/shared-kernel/src/main/java/com/rag/app/shared/infrastructure/knowledge/mappers/KnowledgeGraphMapper.java`
- `backend/shared-kernel/src/main/java/com/rag/app/shared/infrastructure/knowledge/mappers/KnowledgeNodeMapper.java`
- `backend/shared-kernel/src/main/java/com/rag/app/shared/infrastructure/knowledge/mappers/KnowledgeRelationshipMapper.java`
- `backend/shared-kernel/src/test/java/com/rag/app/shared/infrastructure/knowledge/Neo4jKnowledgeGraphRepositoryTest.java`

## Impact
Knowledge graph use cases can now persist graph metadata, nodes, and relationships through a Neo4j repository implementation with reusable Cypher queries and mapper-based domain reconstruction.

## Verification
- `./gradlew :backend:shared-kernel:build :backend:document-management:build`
- `./gradlew :backend:shared-kernel:build :backend:document-management:build`
- `./gradlew :backend:shared-kernel:build :backend:document-management:build`
- `./gradlew :backend:shared-kernel:build :backend:document-management:build`

## Follow-ups
- Add real Neo4j integration tests against a running database or testcontainer.
- Wire the repository and configuration into application runtime composition.
