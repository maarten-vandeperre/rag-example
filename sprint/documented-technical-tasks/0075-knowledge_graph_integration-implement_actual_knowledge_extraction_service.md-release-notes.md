## Summary
Implemented a real heuristic knowledge extraction service and document quality validator, then wired them into backend knowledge-graph configuration for runtime use.

## Changes
- `backend/shared-kernel/src/main/java/com/rag/app/shared/infrastructure/knowledge/HeuristicDocumentQualityValidator.java`
- `backend/shared-kernel/src/main/java/com/rag/app/shared/infrastructure/knowledge/HeuristicKnowledgeExtractionService.java`
- `backend/shared-kernel/src/test/java/com/rag/app/shared/infrastructure/knowledge/HeuristicKnowledgeExtractionServiceTest.java`
- `backend/src/main/java/com/rag/app/config/KnowledgeGraphConfiguration.java`

## Impact
Knowledge extraction now performs actual English-focused heuristic analysis over document sections, entities, concepts, and relationships with confidence filtering, chunk-aware processing, and runtime quality validation instead of relying only on test stubs.

## Verification
- `./gradlew :backend:shared-kernel:build`
- `./gradlew :backend:build`

## Follow-ups
- Replace heuristic extraction with model-backed NLP components when external model dependencies are introduced.
- Add production configuration knobs for extraction strategy, thresholds, and section handling through application properties.
