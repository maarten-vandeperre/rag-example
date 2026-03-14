## Summary
Created the root Gradle multi-project configuration, workspace properties, and Gradle wrapper so the repository can be orchestrated consistently across backend and frontend modules.

## Changes
Added `build.gradle`.
Added `settings.gradle`.
Added `gradle.properties`.
Added `gradlew`.
Added `gradlew.bat`.
Added `gradle/wrapper/gradle-wrapper.properties`.
Added `gradle/wrapper/gradle-wrapper.jar`.

## Impact
The workspace now has a standard Gradle entrypoint for shared project discovery and root-level build lifecycle tasks while preserving the existing Maven and npm module workflows.

## Verification
Ran `JAVA_HOME="/Users/maartenvandeperre/.sdkman/candidates/java/17.0.2-open" ./gradlew --version`.
Ran `JAVA_HOME="/Users/maartenvandeperre/.sdkman/candidates/java/17.0.2-open" ./gradlew projects`.
Ran `JAVA_HOME="/Users/maartenvandeperre/.sdkman/candidates/java/17.0.2-open" ./gradlew clean`.
Ran `JAVA_HOME="/Users/maartenvandeperre/.sdkman/candidates/java/17.0.2-open" mvn -q -Dquarkus.platform.group-id=io.quarkus -Dquarkus.platform.artifact-id=quarkus-bom -Dquarkus.platform.version=2.16.5.Final -DskipTests compile`.
Ran `JAVA_HOME="/Users/maartenvandeperre/.sdkman/candidates/java/17.0.2-open" mvn -q -Dquarkus.platform.group-id=io.quarkus -Dquarkus.platform.artifact-id=quarkus-bom -Dquarkus.platform.version=2.16.5.Final test`.
Ran `CI=true npm test -- --watch=false`.
Ran `npm run build`.

## Follow-ups
Add module-specific Gradle build files for `backend` and `frontend` so the new root workspace tasks can execute real build logic through Gradle end to end.
