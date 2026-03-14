## Summary
Configured Gradle build and packaging workflows for backend and frontend modules, including artifact verification and a root release archive.

## Changes
Updated `backend/build.gradle`.
Updated `frontend/build.gradle`.
Updated `build.gradle`.
Updated `gradle.properties`.

## Impact
The workspace now produces verified backend JAR artifacts, validated frontend production bundles, and a single distributable release zip through Gradle.

## Verification
Ran `JAVA_HOME="/Users/maartenvandeperre/.sdkman/candidates/java/17.0.2-open" ./gradlew :backend:test`.
Ran `JAVA_HOME="/Users/maartenvandeperre/.sdkman/candidates/java/17.0.2-open" ./gradlew :frontend:test`.
Ran `JAVA_HOME="/Users/maartenvandeperre/.sdkman/candidates/java/17.0.2-open" ./gradlew buildWorkspace`.
Ran `JAVA_HOME="/Users/maartenvandeperre/.sdkman/candidates/java/17.0.2-open" ./gradlew packageRelease`.

## Follow-ups
Address the Quarkus-reserved configuration deprecation warnings so backend packaging remains Gradle 9 compatible.
