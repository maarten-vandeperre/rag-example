## Summary
Added shared-kernel knowledge extraction use cases, repository/service interfaces, and test coverage for quality validation, extraction orchestration, and graph build/extension flows.

## Changes
- `backend/shared-kernel/src/main/java/com/rag/app/shared/interfaces/knowledge/DocumentQualityResult.java`
- `backend/shared-kernel/src/main/java/com/rag/app/shared/interfaces/knowledge/DocumentQualityValidator.java`
- `backend/shared-kernel/src/main/java/com/rag/app/shared/interfaces/knowledge/KnowledgeExtractionException.java`
- `backend/shared-kernel/src/main/java/com/rag/app/shared/interfaces/knowledge/UnsupportedDocumentFormatException.java`
- `backend/shared-kernel/src/main/java/com/rag/app/shared/interfaces/knowledge/KnowledgeExtractionService.java`
- `backend/shared-kernel/src/main/java/com/rag/app/shared/interfaces/knowledge/KnowledgeGraphRepository.java`
- `backend/shared-kernel/src/main/java/com/rag/app/shared/usecases/knowledge/ValidateKnowledgeQuality.java`
- `backend/shared-kernel/src/main/java/com/rag/app/shared/usecases/knowledge/ExtractKnowledgeFromDocument.java`
- `backend/shared-kernel/src/main/java/com/rag/app/shared/usecases/knowledge/BuildKnowledgeGraph.java`
- `backend/shared-kernel/src/main/java/com/rag/app/shared/usecases/knowledge/ExtendExistingKnowledgeGraph.java`
- `backend/shared-kernel/src/main/java/com/rag/app/shared/usecases/knowledge/models/KnowledgeExtractionStatus.java`
- `backend/shared-kernel/src/main/java/com/rag/app/shared/usecases/knowledge/models/ExtractKnowledgeInput.java`
- `backend/shared-kernel/src/main/java/com/rag/app/shared/usecases/knowledge/models/ExtractKnowledgeOutput.java`
- `backend/shared-kernel/src/main/java/com/rag/app/shared/usecases/knowledge/models/BuildKnowledgeGraphInput.java`
- `backend/shared-kernel/src/main/java/com/rag/app/shared/usecases/knowledge/models/BuildKnowledgeGraphOutput.java`
- `backend/shared-kernel/src/main/java/com/rag/app/shared/usecases/knowledge/models/ExtendKnowledgeGraphInput.java`
- `backend/shared-kernel/src/main/java/com/rag/app/shared/usecases/knowledge/models/ExtendKnowledgeGraphOutput.java`
- `backend/shared-kernel/src/main/java/com/rag/app/shared/usecases/knowledge/models/ValidateKnowledgeQualityInput.java`
- `backend/shared-kernel/src/main/java/com/rag/app/shared/usecases/knowledge/models/ValidateKnowledgeQualityOutput.java`
- `backend/shared-kernel/src/test/java/com/rag/app/shared/usecases/knowledge/KnowledgeExtractionUseCasesTest.java`

## Impact
The shared kernel now provides framework-agnostic orchestration for validating document quality, extracting knowledge, and creating or extending knowledge graphs, enabling later pipeline and persistence integration.

## Verification
- `./gradlew :backend:shared-kernel:build`
- `./gradlew :backend:shared-kernel:build`

## Follow-ups
- Add infrastructure adapters for real extraction engines and graph persistence.
- Wire the new use cases into the document upload pipeline and administration APIs.
