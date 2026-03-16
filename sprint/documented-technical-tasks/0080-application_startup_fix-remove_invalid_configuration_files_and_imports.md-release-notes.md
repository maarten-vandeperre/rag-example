## Summary
Removed two obsolete backend classes that were flagged as invalid configuration/API wiring so the application no longer carries unused startup-time document processing configuration.

## Changes
- Deleted `backend/src/main/java/com/rag/app/config/DocumentProcessingConfiguration.java`
- Deleted `backend/src/main/java/com/rag/app/api/DocumentProcessingResource.java`

## Impact
Reduces dead backend wiring and removes unused application startup components without affecting the current backend compile or test behavior.

## Verification
- `./gradlew :backend:compileJava`
- `./gradlew :backend:test`
- `./gradlew :backend:compileJava :backend:test`

## Follow-ups
- If document reprocessing administration is still needed, reintroduce it through currently supported module interfaces and verified CDI wiring.
