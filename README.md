# rag-example

## Development setup

- Run `./setup.sh` on macOS/Linux or `setup.bat` on Windows to validate Java/Node, run the Gradle health check, and install frontend dependencies.
- Use `./gradlew dev` to start the workspace development flow, `./gradlew testAll` to run tests, and `./gradlew buildWorkspace` to produce verified build outputs.
- Copy `.env.example` to your local environment file or shell profile if you need to override Java, Gradle, database, or dev-port defaults.

## IDE support

- VS Code settings, tasks, and debugger launch config live in `.vscode/`.
- IntelliJ Gradle wrapper settings live in `.idea/gradle.xml`.
