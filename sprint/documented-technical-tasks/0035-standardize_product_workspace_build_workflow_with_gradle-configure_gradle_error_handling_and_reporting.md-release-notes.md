## Summary
Added Gradle-side error handling, diagnostics, and environment validation so workspace failures surface clearer troubleshooting and escalation details.

## Changes
Updated `build.gradle`.
Updated `backend/build.gradle`.
Updated `frontend/build.gradle`.

## Impact
Workspace builds now emit consistent task lifecycle logging, formatted failure guidance, environment diagnostics, and module-specific startup and test summaries to speed up troubleshooting.

## Verification
Ran `JAVA_HOME="/Users/maartenvandeperre/.sdkman/candidates/java/17.0.2-open" ./gradlew healthCheck`.
Ran `JAVA_HOME="/Users/maartenvandeperre/.sdkman/candidates/java/17.0.2-open" ./gradlew diagnostics`.
Ran `JAVA_HOME="/Users/maartenvandeperre/.sdkman/candidates/java/17.0.2-open" ./gradlew :backend:test`.
Ran `JAVA_HOME="/Users/maartenvandeperre/.sdkman/candidates/java/17.0.2-open" ./gradlew :frontend:test`.
Ran `JAVA_HOME="/Users/maartenvandeperre/.sdkman/candidates/java/17.0.2-open" ./gradlew verifyAll`.

## Follow-ups
Refine the health check dependency-resolution strategy to avoid Gradle 9 unsafe-resolution deprecation warnings while keeping dependency validation in place.
