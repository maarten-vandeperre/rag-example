## Summary
Added root Gradle workspace orchestration tasks for full-stack setup, development, testing, verification, build, cleanup, and release preparation.

## Changes
Updated `build.gradle`.

## Impact
Developers can now use a single Gradle entrypoint to prepare the workspace, coordinate backend and frontend dev flows, run shared verification, and assemble release-ready outputs.

## Verification
Ran `JAVA_HOME="/Users/maartenvandeperre/.sdkman/candidates/java/17.0.2-open" ./gradlew setup`.
Ran `JAVA_HOME="/Users/maartenvandeperre/.sdkman/candidates/java/17.0.2-open" ./gradlew dev --dry-run`.
Ran `JAVA_HOME="/Users/maartenvandeperre/.sdkman/candidates/java/17.0.2-open" ./gradlew testAll`.
Ran `JAVA_HOME="/Users/maartenvandeperre/.sdkman/candidates/java/17.0.2-open" ./gradlew verifyAll`.
Ran `JAVA_HOME="/Users/maartenvandeperre/.sdkman/candidates/java/17.0.2-open" ./gradlew prepareRelease -x :backend:quarkusBuild`.

## Follow-ups
Revisit root release orchestration once `:backend:quarkusBuild` is stable enough to run inside the default backend build lifecycle without exclusions.
