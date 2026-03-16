## Summary
Added shared-kernel knowledge graph domain entities, value objects, and a domain service to model extracted knowledge from uploaded documents.

## Changes
- `backend/shared-kernel/src/main/java/com/rag/app/shared/domain/knowledge/valueobjects/NodeId.java`
- `backend/shared-kernel/src/main/java/com/rag/app/shared/domain/knowledge/valueobjects/RelationshipId.java`
- `backend/shared-kernel/src/main/java/com/rag/app/shared/domain/knowledge/valueobjects/GraphId.java`
- `backend/shared-kernel/src/main/java/com/rag/app/shared/domain/knowledge/valueobjects/NodeType.java`
- `backend/shared-kernel/src/main/java/com/rag/app/shared/domain/knowledge/valueobjects/RelationshipType.java`
- `backend/shared-kernel/src/main/java/com/rag/app/shared/domain/knowledge/valueobjects/ConfidenceScore.java`
- `backend/shared-kernel/src/main/java/com/rag/app/shared/domain/knowledge/valueobjects/DocumentReference.java`
- `backend/shared-kernel/src/main/java/com/rag/app/shared/domain/knowledge/valueobjects/GraphMetadata.java`
- `backend/shared-kernel/src/main/java/com/rag/app/shared/domain/knowledge/valueobjects/ExtractionMetadata.java`
- `backend/shared-kernel/src/main/java/com/rag/app/shared/domain/knowledge/valueobjects/ExtractedKnowledge.java`
- `backend/shared-kernel/src/main/java/com/rag/app/shared/domain/knowledge/entities/KnowledgeNode.java`
- `backend/shared-kernel/src/main/java/com/rag/app/shared/domain/knowledge/entities/KnowledgeRelationship.java`
- `backend/shared-kernel/src/main/java/com/rag/app/shared/domain/knowledge/entities/KnowledgeGraph.java`
- `backend/shared-kernel/src/main/java/com/rag/app/shared/domain/knowledge/services/KnowledgeGraphDomainService.java`
- `backend/shared-kernel/src/test/java/com/rag/app/shared/domain/knowledge/KnowledgeGraphDomainModelTest.java`

## Impact
The shared kernel now exposes pure Java knowledge graph domain primitives with validation, immutable graph updates, and merge rules that can be reused by document upload and extraction workflows.

## Verification
- `./gradlew :backend:shared-kernel:build`

## Follow-ups
- Extend merge rules with semantic similarity heuristics and multi-document provenance aggregation.
- Integrate the new domain models into knowledge extraction use cases and persistence adapters.
