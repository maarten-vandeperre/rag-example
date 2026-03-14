## Summary
Added a Gradle-based frontend module build that wires npm installation, React development mode, linting, test execution, and production bundle creation into the workspace wrapper.

## Changes
Added `frontend/build.gradle`.
Added `frontend/.eslintrc.json`.
Updated `frontend/package.json`.

## Impact
The frontend can now be built and verified through Gradle with consistent Node task orchestration while preserving the existing React scripts workflow.

## Verification
Ran `JAVA_HOME="/Users/maartenvandeperre/.sdkman/candidates/java/17.0.2-open" ./gradlew :frontend:frontendTest`.
Ran `JAVA_HOME="/Users/maartenvandeperre/.sdkman/candidates/java/17.0.2-open" ./gradlew :frontend:lint`.
Ran `JAVA_HOME="/Users/maartenvandeperre/.sdkman/candidates/java/17.0.2-open" ./gradlew :frontend:build`.
Ran `JAVA_HOME="/Users/maartenvandeperre/.sdkman/candidates/java/17.0.2-open" ./gradlew :frontend:dev --dry-run`.
Ran `JAVA_HOME="/Users/maartenvandeperre/.sdkman/candidates/java/17.0.2-open" mvn -q -Dquarkus.platform.group-id=io.quarkus -Dquarkus.platform.artifact-id=quarkus-bom -Dquarkus.platform.version=2.16.5.Final -DskipTests compile`.
Ran `JAVA_HOME="/Users/maartenvandeperre/.sdkman/candidates/java/17.0.2-open" mvn -q -Dquarkus.platform.group-id=io.quarkus -Dquarkus.platform.artifact-id=quarkus-bom -Dquarkus.platform.version=2.16.5.Final test`.

## Follow-ups
Consider adding a frontend Gradle task for Docker-oriented builds if the workspace needs parity with the existing `build:docker` npm script.
