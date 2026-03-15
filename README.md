# rag-example

## Development setup

- Run `./setup.sh` on macOS/Linux or `setup.bat` on Windows to validate Java/Node, run the Gradle health check, and install frontend dependencies.
- Use `./gradlew dev` to start the workspace development flow, `./gradlew testAll` to run tests, and `./gradlew buildWorkspace` to produce verified build outputs.
- Copy `.env.example` to your local environment file or shell profile if you need to override Java, Gradle, database, or dev-port defaults.
- Use `./start-dev-services.sh` with `docker-compose.dev.yml` and `.env.dev` to start Podman-based supporting services only, then run backend/frontend separately.
- Use `./status-dev-services.sh` to inspect service health and `./stop-dev-services.sh` to tear the development services down.
- Use `./infrastructure/database/reset-dev-db.sh`, `./infrastructure/database/backup-dev-db.sh`, and `./infrastructure/database/status-dev-db.sh` to manage the local development PostgreSQL dataset.
- Use `./infrastructure/weaviate/init-weaviate-dev.sh`, `./infrastructure/weaviate/load-sample-data.sh`, `./infrastructure/weaviate/test-vector-search.sh`, and `./infrastructure/weaviate/manage-weaviate-dev.sh` to initialize and inspect local Weaviate development data.
- Full local-development documentation lives in `docs/development/README.md` with supporting workflow, troubleshooting, service-management, and onboarding guides.

## IDE support

- VS Code settings, tasks, and debugger launch config live in `.vscode/`.
- IntelliJ Gradle wrapper settings live in `.idea/gradle.xml`.
