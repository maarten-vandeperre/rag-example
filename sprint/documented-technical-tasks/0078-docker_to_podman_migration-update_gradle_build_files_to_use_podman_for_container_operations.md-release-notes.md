## Summary
Updated Gradle build scripts to use Podman for compose validation, image builds, and local service lifecycle tasks.

## Changes
- `build.gradle`
- `backend/build.gradle`
- `frontend/build.gradle`
- `backend/Dockerfile`
- `src/test/integration/java/gradle/DevelopmentModeTest.java`

## Impact
The workspace now provides Podman-native Gradle tasks for validating compose files, building backend/frontend/database images, and managing local service workflows without relying on Docker-specific commands.

## Verification
- `./gradlew healthCheck`
- `./gradlew verifyPodmanContainerWorkflow`
- `./gradlew :backend:shared-kernel:test :frontend:test`

## Follow-ups
- Add dedicated Gradle integration coverage for the new Podman tasks when the root integration test source set is wired in.
- Review remaining Dockerfile/container build assumptions in backend packaging for future registry publishing tasks.
