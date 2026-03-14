## Summary
Standardized the Gradle wrapper and added repository setup assets so developers and IDEs can bootstrap the workspace with a consistent build environment.

## Changes
Updated `gradle/wrapper/gradle-wrapper.properties`.
Updated `gradlew`.
Updated `gradlew.bat`.
Updated `README.md`.
Updated `gradle.properties`.
Added `setup.sh`.
Added `setup.bat`.
Added `.env.example`.
Added `.vscode/settings.json`.
Added `.vscode/launch.json`.
Added `.vscode/tasks.json`.
Added `.idea/gradle.xml`.

## Impact
The workspace now boots through a pinned Gradle 8.5 wrapper, includes cross-platform setup scripts, and provides baseline VS Code and IntelliJ configuration for faster onboarding.

## Verification
Ran `JAVA_HOME="/Users/maartenvandeperre/.sdkman/candidates/java/17.0.2-open" ./gradlew wrapper --gradle-version 8.5 --distribution-type all`.
Ran `JAVA_HOME="/Users/maartenvandeperre/.sdkman/candidates/java/17.0.2-open" ./gradlew --version`.
Ran `JAVA_HOME="/Users/maartenvandeperre/.sdkman/candidates/java/17.0.2-open" ./setup.sh`.
Ran `JAVA_HOME="/Users/maartenvandeperre/.sdkman/candidates/java/17.0.2-open" ./gradlew verifyAll`.

## Follow-ups
Refine health-check dependency validation to avoid Gradle unsafe-resolution deprecation warnings during setup runs.
