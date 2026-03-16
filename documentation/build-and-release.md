# Build And Release

## Workspace layout

The root Gradle build in `build.gradle` manages two subprojects:

- `backend`
- `frontend`

`settings.gradle` includes both modules, and `gradle.properties` carries shared workspace metadata for the Java 25-based toolchain.

## Daily commands

```bash
./gradlew setup
./gradlew dev
./gradlew integrationTest
./gradlew testAll
./gradlew testAllWithReport
./gradlew buildWorkspace
./gradlew packageRelease
./gradlew :backend:integration-tests:test
./gradlew :frontend:lint
```

## Backend Gradle tasks

```bash
./gradlew :backend:dev
./gradlew :backend:test
./gradlew :backend:unitTest
./gradlew :backend:integrationTest
./gradlew :backend:packageBackend
./gradlew :backend:fatJar
./gradlew :backend:verifyBuild
./gradlew :backend:verify
```

What they do:

- `dev` starts Quarkus development mode
- `test` runs non-integration tests
- `integrationTest` runs tests tagged `integration`
- `packageBackend` builds the classes JAR and fat JAR
- `verifyBuild` checks both backend artifacts exist

Modular backend validation also includes:

```bash
./gradlew :backend:integration-tests:test
```

This suite covers cross-module workflow, boundary, event, health, and compatibility checks using fixtures and stubs.

Example:

```bash
./gradlew :backend:integrationTest
```

## Frontend Gradle tasks

```bash
./gradlew :frontend:npmInstall
./gradlew :frontend:dev
./gradlew :frontend:test
./gradlew :frontend:testComponents
./gradlew :frontend:lint
./gradlew :frontend:buildProd
./gradlew :frontend:analyzeBuild
./gradlew :frontend:verifyBuild
./gradlew :frontend:verify
```

Notes:

- Node.js 18 is recommended and checked during install
- frontend tests emit coverage plus JUnit XML output
- `verifyBuild` checks that `build/index.html` exists
- `npm run lint:all` enforces the newer frontend module-boundary rules

## Workspace orchestration tasks

```bash
./gradlew healthCheck
./gradlew integrationTest
./gradlew diagnostics
./gradlew cleanAll
./gradlew buildAll
./gradlew verifyAll
./gradlew quickBuild
./gradlew prepareRelease
./gradlew buildWorkspace
./gradlew verifyPodmanTooling
./gradlew validatePodmanComposeFiles
./gradlew verifyPodmanContainerWorkflow
```

Important behavior:

- `healthCheck` validates Java version, module structure, and dependency resolution
- `integrationTest` runs root-level Gradle workflow integration tests from `src/test/integration/java/gradle/`
- `diagnostics` prints environment and task metadata for escalation
- `quickBuild` packages the backend and builds the production frontend bundle
- `buildWorkspace` verifies backend and frontend build outputs together
- `prepareRelease` runs verification before building release outputs
- `verifyPodmanTooling` checks that `podman` and `podman-compose` are installed
- `validatePodmanComposeFiles` runs Podman Compose config validation for both compose files
- `verifyPodmanContainerWorkflow` validates compose files and builds the backend, frontend, and database images through Podman

## Gradle workflow integration tests

The root `integrationTest` task validates the workspace build flow end to end after prerequisite tasks have produced artifacts.

What it covers:

- environment setup via `healthCheck`
- frontend dependency installation assumptions
- aggregated test report generation
- backend and frontend build outputs
- release archive generation
- diagnostics and failure-banner behavior
- development task wiring through `./gradlew dev --dry-run`

Implementation location:

- `src/test/integration/java/gradle/WorkflowIntegrationTest.java`
- `src/test/integration/java/gradle/BuildProcessTest.java`
- `src/test/integration/java/gradle/DevelopmentModeTest.java`
- `src/test/integration/java/gradle/ErrorHandlingTest.java`
- `src/test/integration/java/gradle/GradleCommandRunner.java`

Example:

```bash
./gradlew --no-daemon integrationTest
```

Notes:

- the test runner invokes nested Gradle commands with `--no-parallel` and `--max-workers=1` for stability
- release archive assertions locate artifacts dynamically instead of relying on hash-specific asset names
- the development-mode coverage currently verifies task wiring, not a long-running live startup session

## Native development commands

Supporting services:

```bash
./start-dev-services.sh
./status-dev-services.sh
./troubleshoot-dev-services.sh
./stop-dev-services.sh
```

Notes:

- the lifecycle scripts use `podman-compose`
- `./stop-dev-services.sh --clean` removes persisted local dev volumes
- the compose files are maintained for Podman Compose compatibility in both `docker-compose.yml` and `docker-compose.dev.yml`

Container build tasks are Podman-native:

```bash
./gradlew buildDatabaseContainerImage
./gradlew :backend:buildContainerImage
./gradlew :frontend:buildContainerImage
./gradlew buildContainerImages
./gradlew verifyPodmanContainerWorkflow
```

Backend native dev:

```bash
cd backend && ./start-dev.sh
cd backend && ./test-dev-integration.sh
```

Frontend native dev:

```bash
cd frontend && ./start-dev.sh
cd frontend && npm run start:dev
cd frontend && npm run test:dev
```

## Test reporting

Aggregated test reports are generated under `build/reports/allTests`.

Example:

```bash
./gradlew testAllWithReport
```

Outputs:

- backend aggregated report: `build/reports/allTests/backend/`
- frontend summary: `build/reports/allTests/frontend/index.html`

Backend test split:

- unit tests: default `test` and `unitTest`
- integration tests: `integrationTest`

Notable backend integration coverage now includes an end-to-end workflow test for:

- document upload
- document processing
- chat query execution
- knowledge graph browsing and search

## Release outputs

Backend artifacts:

- classes JAR in `backend/build/libs/`
- fat JAR in `backend/build/libs/`

Frontend artifact:

- production bundle in `frontend/build/`

Release packaging:

```bash
./gradlew packageRelease
```

The release zip includes:

- backend libs
- frontend build output
- `README.md`
- `LICENSE`
- `docker-compose.yml`
- `gradlew`, `gradlew.bat`
- `gradle/`

## CI workflow

The repository now includes a Podman-based GitHub Actions workflow:

- `.github/workflows/podman-ci.yml`

What it verifies:

- Java 25 and Node.js 18 setup
- workspace `healthCheck`
- targeted `:backend:shared-kernel:test` and `:frontend:test`
- `verifyPodmanContainerWorkflow`
- helper-script syntax checks and Podman Compose validation for both compose files

## Legacy build paths

The backend Maven build in `backend/pom.xml` still exists for module-level work, but the documented workspace flow is Gradle-first.
