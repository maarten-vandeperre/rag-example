## Summary
Added end-to-end backend integration tests that exercise upload, processing, chat, access control, admin progress, and failure scenarios using shared in-memory test wiring.

## Changes
Added `backend/src/test/java/integration/IntegrationTestSupport.java`.
Added `backend/src/test/java/integration/DocumentUploadIntegrationTest.java`.
Added `backend/src/test/java/integration/RoleBasedAccessIntegrationTest.java`.
Added `backend/src/test/java/integration/ChatQueryIntegrationTest.java`.
Added `backend/src/test/java/integration/ErrorScenarioIntegrationTest.java`.
Added `backend/src/test/java/integration/AdminProgressIntegrationTest.java`.
Added `backend/src/test/resources/test-documents/knowledge-base.md`.
Added `backend/src/test/resources/test-documents/operations.txt`.

## Impact
The document upload and private knowledge-base workflow now has regression coverage across the full application stack, including role-aware access and processing failure paths.

## Verification
Ran `mvn -q -Dquarkus.platform.group-id=io.quarkus -Dquarkus.platform.artifact-id=quarkus-bom -Dquarkus.platform.version=2.16.5.Final -DskipTests compile`.
Ran `mvn -q -Dquarkus.platform.group-id=io.quarkus -Dquarkus.platform.artifact-id=quarkus-bom -Dquarkus.platform.version=2.16.5.Final test`.

## Follow-ups
Replace the in-memory test harness with container-backed database, vector store, and provider-based LLM integration tests when those runtime dependencies are available in CI.
