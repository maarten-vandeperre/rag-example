## Summary
Configured Gradle-driven test execution for backend and frontend modules with category-aware tasks, coverage/report generation, and root-level report aggregation.

## Changes
Updated `backend/build.gradle`.
Updated `build.gradle`.
Updated `frontend/build.gradle`.
Updated `frontend/package.json`.
Updated `frontend/package-lock.json`.
Updated `backend/src/test/java/integration/AdminProgressIntegrationTest.java`.
Updated `backend/src/test/java/integration/ChatQueryIntegrationTest.java`.
Updated `backend/src/test/java/integration/DocumentUploadIntegrationTest.java`.
Updated `backend/src/test/java/integration/ErrorScenarioIntegrationTest.java`.
Updated `backend/src/test/java/integration/RoleBasedAccessIntegrationTest.java`.
Added `backend/src/test/resources/junit-platform.properties`.

## Impact
The workspace now supports separate backend unit and integration runs, frontend Jest reporting with coverage and JUnit XML, and a single Gradle entrypoint for aggregated test reporting.

## Verification
Ran `JAVA_HOME="/Users/maartenvandeperre/.sdkman/candidates/java/17.0.2-open" ./gradlew :backend:test`.
Ran `JAVA_HOME="/Users/maartenvandeperre/.sdkman/candidates/java/17.0.2-open" ./gradlew :backend:unitTest`.
Ran `JAVA_HOME="/Users/maartenvandeperre/.sdkman/candidates/java/17.0.2-open" ./gradlew :backend:integrationTest`.
Ran `JAVA_HOME="/Users/maartenvandeperre/.sdkman/candidates/java/17.0.2-open" ./gradlew :frontend:test`.
Ran `JAVA_HOME="/Users/maartenvandeperre/.sdkman/candidates/java/17.0.2-open" ./gradlew :frontend:testComponents`.
Ran `JAVA_HOME="/Users/maartenvandeperre/.sdkman/candidates/java/17.0.2-open" ./gradlew :frontend:build`.
Ran `JAVA_HOME="/Users/maartenvandeperre/.sdkman/candidates/java/17.0.2-open" ./gradlew testAllWithReport`.
Ran `JAVA_HOME="/Users/maartenvandeperre/.sdkman/candidates/java/17.0.2-open" ./gradlew verifyAll`.

## Follow-ups
Resolve the duplicate `junit-platform.properties` classpath warning from Quarkus test dependencies if stricter JUnit platform configuration ownership is needed later.
