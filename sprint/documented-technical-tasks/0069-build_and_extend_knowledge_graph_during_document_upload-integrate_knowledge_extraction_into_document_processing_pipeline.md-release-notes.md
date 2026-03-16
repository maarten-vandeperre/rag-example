## Summary
Integrated knowledge extraction into the document-processing pipeline so search indexing and knowledge graph updates run together with separate status tracking and non-blocking failure handling.

## Changes
- `backend/document-management/build.gradle`
- `backend/shared-kernel/src/main/java/com/rag/app/shared/configuration/KnowledgeProcessingConfiguration.java`
- `backend/document-management/src/main/java/com/rag/app/document/domain/valueobjects/KnowledgeProcessingStatus.java`
- `backend/document-management/src/main/java/com/rag/app/document/domain/entities/Document.java`
- `backend/document-management/src/main/java/com/rag/app/document/usecases/ProcessDocument.java`
- `backend/document-management/src/main/java/com/rag/app/document/usecases/models/ProcessDocumentOutput.java`
- `backend/document-management/src/main/java/com/rag/app/document/usecases/models/SearchProcessingResult.java`
- `backend/document-management/src/main/java/com/rag/app/document/usecases/models/KnowledgeProcessingResult.java`
- `backend/document-management/src/test/java/com/rag/app/document/DocumentManagementFacadeTest.java`
- `backend/document-management/src/test/java/com/rag/app/document/usecases/ProcessDocumentKnowledgeIntegrationTest.java`
- `backend/src/main/resources/schema.sql`

## Impact
Document processing now preserves search availability when knowledge extraction fails, records knowledge-processing lifecycle metadata on documents, and supports per-document-type configuration for knowledge graph enrichment.

## Verification
- `./gradlew :backend:shared-kernel:build :backend:document-management:build`
- `./gradlew :backend:shared-kernel:build :backend:document-management:build`
- `./gradlew :backend:shared-kernel:build :backend:document-management:build`

## Follow-ups
- Persist the new knowledge-processing document fields through concrete repository adapters.
- Replace default async execution with a dedicated executor when pipeline throughput tuning is needed.
