## Summary
Added shared-kernel browsing, search, and statistics use cases plus an application-integration knowledge graph administration controller and DTOs for admin-facing graph exploration APIs.

## Changes
- `backend/shared-kernel/src/main/java/com/rag/app/shared/usecases/knowledge/BrowseKnowledgeGraph.java`
- `backend/shared-kernel/src/main/java/com/rag/app/shared/usecases/knowledge/SearchKnowledgeGraph.java`
- `backend/shared-kernel/src/main/java/com/rag/app/shared/usecases/knowledge/GetKnowledgeGraphStatistics.java`
- `backend/shared-kernel/src/main/java/com/rag/app/shared/usecases/knowledge/models/BrowseType.java`
- `backend/shared-kernel/src/main/java/com/rag/app/shared/usecases/knowledge/models/BrowseKnowledgeGraphInput.java`
- `backend/shared-kernel/src/main/java/com/rag/app/shared/usecases/knowledge/models/BrowseKnowledgeGraphOutput.java`
- `backend/shared-kernel/src/main/java/com/rag/app/shared/usecases/knowledge/models/SearchKnowledgeGraphInput.java`
- `backend/shared-kernel/src/main/java/com/rag/app/shared/usecases/knowledge/models/SearchKnowledgeGraphOutput.java`
- `backend/shared-kernel/src/main/java/com/rag/app/shared/usecases/knowledge/models/GetKnowledgeGraphStatisticsInput.java`
- `backend/shared-kernel/src/main/java/com/rag/app/shared/usecases/knowledge/models/GetKnowledgeGraphStatisticsOutput.java`
- `backend/shared-kernel/src/test/java/com/rag/app/shared/usecases/knowledge/KnowledgeGraphAdministrationUseCasesTest.java`
- `backend/application-integration/src/main/java/com/rag/app/integration/api/controllers/KnowledgeGraphController.java`
- `backend/application-integration/src/main/java/com/rag/app/integration/api/dto/knowledge/KnowledgeGraphDtoMapper.java`
- `backend/application-integration/src/main/java/com/rag/app/integration/api/dto/knowledge/KnowledgeGraphDto.java`
- `backend/application-integration/src/main/java/com/rag/app/integration/api/dto/knowledge/KnowledgeGraphSummaryDto.java`
- `backend/application-integration/src/main/java/com/rag/app/integration/api/dto/knowledge/KnowledgeGraphMetadataDto.java`
- `backend/application-integration/src/main/java/com/rag/app/integration/api/dto/knowledge/KnowledgeNodeDto.java`
- `backend/application-integration/src/main/java/com/rag/app/integration/api/dto/knowledge/KnowledgeRelationshipDto.java`
- `backend/application-integration/src/main/java/com/rag/app/integration/api/dto/knowledge/KnowledgeNodeDetailDto.java`
- `backend/application-integration/src/main/java/com/rag/app/integration/api/dto/knowledge/KnowledgeDocumentReferenceDto.java`
- `backend/application-integration/src/main/java/com/rag/app/integration/api/dto/knowledge/KnowledgeSubgraphDto.java`
- `backend/application-integration/src/main/java/com/rag/app/integration/api/dto/knowledge/KnowledgeSearchResultDto.java`
- `backend/application-integration/src/main/java/com/rag/app/integration/api/dto/knowledge/KnowledgeGraphStatisticsDto.java`
- `backend/application-integration/src/main/java/com/rag/app/integration/api/dto/knowledge/GraphVisualizationDto.java`
- `backend/application-integration/src/main/java/com/rag/app/integration/api/dto/knowledge/VisualizationNodeDto.java`
- `backend/application-integration/src/main/java/com/rag/app/integration/api/dto/knowledge/VisualizationEdgeDto.java`
- `backend/application-integration/src/main/java/com/rag/app/integration/orchestration/ApplicationOrchestrator.java`
- `backend/application-integration/src/test/java/com/rag/app/integration/KnowledgeGraphControllerTest.java`
- `backend/application-integration/src/test/java/com/rag/app/integration/ApplicationOrchestratorTest.java`

## Impact
Administrators can now list graphs, inspect graph and node details, retrieve visualization-ready subgraphs, search graph content, and view aggregate graph statistics through application-integration APIs backed by reusable shared-kernel use cases.

## Verification
- `./gradlew :backend:shared-kernel:build :backend:document-management:build :backend:application-integration:build`
- `./gradlew :backend:shared-kernel:build :backend:document-management:build :backend:application-integration:build`

## Follow-ups
- Add real HTTP transport bindings and route wiring if these controllers are exposed through Quarkus endpoints.
- Back search relevance and pagination with repository-native Neo4j queries for larger datasets.
