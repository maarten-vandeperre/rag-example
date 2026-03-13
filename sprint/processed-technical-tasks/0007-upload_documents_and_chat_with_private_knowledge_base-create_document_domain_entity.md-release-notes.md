## Summary
Added the initial document domain model for uploaded knowledge-base files, including status tracking, file typing, and metadata validation.

## Changes
- `backend/src/main/java/com/rag/app/domain/entities/Document.java`
- `backend/src/main/java/com/rag/app/domain/valueobjects/DocumentMetadata.java`
- `backend/src/main/java/com/rag/app/domain/valueobjects/DocumentStatus.java`
- `backend/src/main/java/com/rag/app/domain/valueobjects/FileType.java`
- `backend/src/test/java/com/rag/app/domain/entities/DocumentTest.java`
- `backend/src/test/java/com/rag/app/domain/valueobjects/DocumentStatusTest.java`
- `backend/src/test/java/com/rag/app/domain/valueobjects/FileTypeTest.java`
- `backend/src/test/java/com/rag/app/api/BackendStatusResourceTest.java`
- `backend/pom.xml`

## Impact
The backend now has a validated domain representation for uploaded documents that can be reused by future application and persistence layers.

## Verification
- `mvn -s maven-settings.xml -U test`
- `mvn -s maven-settings.xml -U verify` (timed out during Quarkus packaging after compile and tests completed)

## Follow-ups
- Add dedicated document identifier and uploader value objects when the broader domain model is introduced.
- Revisit the Quarkus package build configuration if full `verify` packaging is required in CI without container tooling.
