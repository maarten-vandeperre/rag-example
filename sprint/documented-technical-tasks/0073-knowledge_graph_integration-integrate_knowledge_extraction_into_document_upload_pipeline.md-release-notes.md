## Summary
Integrated knowledge extraction more cleanly into the upload-processing pipeline by adding timeout/retry controls, preserving search availability on knowledge failures, and publishing richer document-processing events without duplicating vector indexing.

## Changes
- `backend/shared-kernel/src/main/java/com/rag/app/shared/configuration/KnowledgeProcessingConfiguration.java`
- `backend/document-management/src/main/java/com/rag/app/document/usecases/ProcessDocument.java`
- `backend/document-management/src/test/java/com/rag/app/document/usecases/ProcessDocumentKnowledgeIntegrationTest.java`
- `backend/application-integration/src/main/java/com/rag/app/integration/events/events/DocumentProcessedEvent.java`
- `backend/application-integration/src/main/java/com/rag/app/integration/orchestration/ApplicationOrchestrator.java`
- `backend/application-integration/src/test/java/com/rag/app/integration/ApplicationOrchestratorTest.java`
- `backend/integration-tests/src/test/java/com/rag/app/integration/support/IntegrationTestFixtures.java`
- `backend/integration-tests/src/test/java/com/rag/app/integration/modules/ModuleCommunicationTest.java`

## Impact
Document uploads now keep search indexing and knowledge processing isolated, support configurable retries and timeouts for knowledge extraction, and expose knowledge-processing metadata through integration events so downstream observers can react without reprocessing document vectors.

## Verification
- `./gradlew :backend:shared-kernel:build :backend:document-management:build`
- `./gradlew :backend:application-integration:build :backend:integration-tests:test`
- `./gradlew :backend:shared-kernel:build :backend:document-management:build :backend:application-integration:build :backend:integration-tests:test :backend:build`

## Follow-ups
- Replace common-pool async execution in document processing with a dedicated executor when throughput tuning is needed.
- Add operational logging/metrics around timed-out and retried knowledge-processing attempts.
