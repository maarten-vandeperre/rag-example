## Summary
Upgraded workspace Java configuration from 17 to 25 across Gradle, Maven, setup, runtime configuration, Docker, and IDE settings, and updated JaCoCo so backend verification still passes on Java 25.

## Changes
Updated `gradle.properties` for Java 25 toolchain and JVM settings.
Updated `backend/build.gradle` to use JaCoCo `0.8.13` for Java 25-compatible coverage reporting.
Updated `backend/pom.xml`, `backend/Dockerfile`, and `backend/src/main/resources/application.properties` for Java 25 build/runtime settings.
Updated `setup.sh` and `.vscode/settings.json` to guide local development with Java 25.

## Impact
The workspace now builds and verifies successfully on Java 25, including backend unit/integration tests, release packaging, and coverage reporting.

## Verification
Executed `./gradlew --no-daemon healthCheck test`.
Executed `./gradlew --no-daemon verifyAll`.

## Follow-ups
Update remaining docs that still mention Java 17 and consider modernizing the reserved Gradle test source-set configuration before upgrading Gradle further.
