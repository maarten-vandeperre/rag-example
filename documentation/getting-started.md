# Getting Started

## What this workspace runs

This project is a private knowledge base application with:

- a Quarkus backend in `backend/`
- a React frontend in `frontend/`
- local infrastructure in `docker-compose.yml`, managed with Podman Compose, for PostgreSQL, Weaviate, Ollama, and the app containers
- Gradle workspace orchestration at the repository root

Important current behavior:

- the native dev stack includes PostgreSQL, Weaviate, Keycloak, Redis, Neo4j, and Ollama
- the development backend can use real Weaviate retrieval, real Neo4j graph persistence, and real Ollama responses
- Gradle is the primary workspace workflow, but the backend Maven build still exists in `backend/pom.xml`

## Prerequisites

- Java 25
- Node.js 18+
- Podman and `podman-compose`

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

- verifies Java 25 and Node.js 18+
- runs `./gradlew healthCheck`
- installs frontend dependencies with `:frontend:npmInstall`

## Native dev environment with Podman services

For day-to-day work, the repository also supports a hybrid local setup where infrastructure runs in containers and the apps run natively.

The dev service scripts use `podman-compose`.

Current expectation: Podman is the primary local container workflow for this repository. Historical Docker-specific instructions should be treated as legacy unless a document explicitly says otherwise.

Start supporting services only:

```bash
./start-dev-services.sh
./status-dev-services.sh
./troubleshoot-dev-services.sh
```

This uses `docker-compose.dev.yml` plus `.env.dev` to start:

- PostgreSQL on `5432`
- Weaviate on `8080`
- Keycloak on `8180`
- Redis on `6379`
- Neo4j Browser on `7474` and Bolt on `7687`
- Ollama on `11434`

Before relying on local auth flows, validate the imported Keycloak realm:

```bash
./infrastructure/keycloak/validate-realm.sh
./infrastructure/keycloak/test-auth.sh
```

Development realm details:

- realm: `rag-app-dev`
- backend client: `rag-app-backend`
- frontend client: `rag-app-frontend`
- seeded users:
  - `john.doe / password123`
  - `jane.admin / admin123`
  - `test.user / test123`

The dev database startup also applies the migration required for chat answer persistence, so local chat queries can store document references and answer-source references successfully.

Startup order is managed automatically:

1. PostgreSQL and Redis
2. Weaviate
3. Weaviate schema bootstrap
4. Keycloak
5. Neo4j
6. Ollama and model bootstrap

If Weaviate needs manual recovery during local startup:

```bash
./infrastructure/weaviate/init-weaviate-dev.sh
./infrastructure/weaviate/troubleshoot-weaviate.sh
```

Then start the apps separately:

```bash
cd backend && ./start-dev.sh
cd frontend && ./start-dev.sh
```

Native dev URLs:

- frontend: `http://localhost:3000`
- backend API: `http://localhost:8081/api`
- Swagger UI: `http://localhost:8081/q/swagger-ui`
- Keycloak admin: `http://localhost:8180/admin`
- Neo4j Browser: `http://localhost:7474`

## Start the local infrastructure stack

```bash
podman-compose -f docker-compose.yml up -d
```

The compose files are now maintained for Podman Compose compatibility, including Podman-friendly volume and network naming and bind-mount options that work better on SELinux-enabled hosts.

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

Backend development mode is verified on Java 25 with the current workspace toolchain. For backend-only startup:

```bash
./gradlew :backend:quarkusDev
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

Native dev defaults from `.env.dev` and `backend/src/main/resources/application-dev.properties`:

```properties
DB_NAME=rag_app_dev
DB_USER=rag_dev_user
DB_PASSWORD=rag_dev_password
WEAVIATE_URL=http://localhost:8080
KEYCLOAK_URL=http://localhost:8180
BACKEND_PORT=8081
FRONTEND_PORT=3000
LLM_MODEL=tinyllama
```

Frontend defaults from `docker-compose.yml`:

```properties
REACT_APP_API_URL=/api
REACT_APP_MAX_FILE_SIZE=41943040
REACT_APP_SUPPORTED_FILE_TYPES=pdf,md,txt
```

## Example local session

1. Run `./setup.sh`
2. Run `podman-compose -f docker-compose.yml up -d`
3. Run `./gradlew dev`
4. Open `http://localhost:3000`
5. Upload a `.pdf`, `.md`, or `.txt` file smaller than 40 MB
6. Query the backend API or embed the chat workspace component

Example native dev session:

1. Run `./start-dev-services.sh`
2. Run `./infrastructure/keycloak/validate-realm.sh`
3. Optionally confirm Ollama with `curl http://localhost:11434/api/tags`
4. Run `cd backend && ./start-dev.sh`
5. Run `cd frontend && ./start-dev.sh`
6. Open `http://localhost:3000`
7. Use the dev auth stub in the browser UI or the seeded Keycloak users for service checks

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
