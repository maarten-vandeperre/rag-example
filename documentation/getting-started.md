# Getting Started

## What this workspace runs

This project is a private knowledge base application with:

- a Quarkus backend in `backend/`
- a React frontend in `frontend/`
- local infrastructure in `docker-compose.yml` for PostgreSQL, Weaviate, and Ollama
- Gradle workspace orchestration at the repository root

Important current behavior:

- the infrastructure stack includes Weaviate and Ollama
- the application logic currently uses an in-memory vector store and a heuristic answer generator
- Gradle is the primary workspace workflow, but the backend Maven build still exists in `backend/pom.xml`

## Prerequisites

- Java 17
- Node.js 18+
- Docker Compose or Podman Compose

Quick checks:

```bash
java -version
node --version
./gradlew --version
```

## First-time setup

On macOS or Linux:

```bash
./setup.sh
```

On Windows:

```bat
setup.bat
```

The setup script:

- verifies Java 17 and Node.js 18+
- runs `./gradlew healthCheck`
- installs frontend dependencies with `:frontend:npmInstall`

## Start the local infrastructure stack

```bash
docker compose up -d
```

Services created by `docker-compose.yml`:

- `postgres` for relational storage
- `weaviate` plus `weaviate-init` for vector schema bootstrap
- `ollama` plus `ollama-init` for local model bootstrap
- `backend`
- `frontend`

Default exposed ports:

- frontend: `http://localhost:3000`
- backend: `http://localhost:8080`
- Ollama: `http://localhost:11434`

PostgreSQL and Weaviate stay internal to the compose network.

## Run the app in workspace development mode

```bash
./gradlew dev
```

This starts:

- `:backend:dev` -> Quarkus development mode
- `:frontend:dev` -> React development server

## Useful Gradle commands

```bash
./gradlew setup
./gradlew dev
./gradlew testAll
./gradlew testAllWithReport
./gradlew buildWorkspace
./gradlew packageRelease
./gradlew diagnostics
```

## Environment configuration

Backend defaults in `backend/src/main/resources/application.properties`:

```properties
DB_USER=rag_user
DB_PASSWORD=rag_password
DB_NAME=rag_app
DB_HOST=postgres
DB_PORT=5432
DOCUMENT_STORAGE_PATH=/app/storage/documents
MAX_FILE_SIZE=41943040
WEAVIATE_URL=http://weaviate:8080
OLLAMA_URL=http://ollama:11434
LLM_PROVIDER=ollama
LLM_MODEL=tinyllama
CORS_ORIGINS=http://localhost:3000
```

Frontend defaults from `docker-compose.yml`:

```properties
REACT_APP_API_URL=/api
REACT_APP_MAX_FILE_SIZE=41943040
REACT_APP_SUPPORTED_FILE_TYPES=pdf,md,txt
```

## Example local session

1. Run `./setup.sh`
2. Run `docker compose up -d`
3. Run `./gradlew dev`
4. Open `http://localhost:3000`
5. Upload a `.pdf`, `.md`, or `.txt` file smaller than 40 MB
6. Query the backend API or embed the chat workspace component

## Health checks

Backend health endpoints:

```bash
curl http://localhost:8080/api/health
curl http://localhost:8080/q/health
```

Example response:

```json
{
  "status": "ok"
}
```
