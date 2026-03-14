## Summary
Added a Gradle-based backend module build for the Quarkus service, including dependency management, development-mode wiring, test configuration, and backend packaging helpers.

## Changes
Added `backend/build.gradle`.
Updated `build.gradle`.
Updated `gradle.properties`.

## Impact
The backend can now be built and tested through the workspace Gradle wrapper with Java 17 toolchains, Quarkus plugin support, a `dev` alias, and a dedicated `packageBackend` packaging task.

## Verification
Ran `JAVA_HOME="/Users/maartenvandeperre/.sdkman/candidates/java/17.0.2-open" ./gradlew :backend:test`.
Ran `JAVA_HOME="/Users/maartenvandeperre/.sdkman/candidates/java/17.0.2-open" ./gradlew :backend:build -x :backend:quarkusBuild`.
Ran `JAVA_HOME="/Users/maartenvandeperre/.sdkman/candidates/java/17.0.2-open" ./gradlew :backend:packageBackend`.
Ran `JAVA_HOME="/Users/maartenvandeperre/.sdkman/candidates/java/17.0.2-open" ./gradlew :backend:dev --dry-run`.

## Follow-ups
Investigate the long-running `:backend:quarkusBuild` task with the Quarkus 2.16 Gradle plugin so fast-jar packaging can be fully reattached to the default Gradle `build` lifecycle.
