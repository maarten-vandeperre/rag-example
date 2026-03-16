# Development Environment

## Overview

The repository supports a hybrid local development workflow:

- supporting services run through `docker-compose.dev.yml`
- backend and frontend run natively for faster iteration

Supporting service stack:

- PostgreSQL
- Weaviate
- Keycloak
- Redis
- Neo4j
- Ollama

Current compose details that matter for debugging:

- PostgreSQL uses `pgvector/pgvector:pg15`
- Weaviate health checks target `http://localhost:8080/v1/meta`
- Keycloak now uses PostgreSQL via `KC_DB=postgres`
- Ollama is optional and starts only when `START_LLM=true`

## Start and stop services

```bash
./start-dev-services.sh
./status-dev-services.sh
./troubleshoot-dev-services.sh
./stop-dev-services.sh
```

The service scripts read `.env.dev` and wait for service readiness before reporting success.

The lifecycle scripts are container-runtime agnostic and try, in order:

- `podman-compose`
- `docker-compose`
- `docker compose`

Startup order:

1. `postgres-dev` and `redis-dev`
2. `weaviate-dev`
3. Weaviate schema initialization through `infrastructure/weaviate/init-weaviate-dev.sh`
4. `keycloak-dev`
5. `neo4j-dev`
6. optional `ollama-dev` when `START_LLM=true`

Required files checked before startup:

- `infrastructure/database/init-dev.sql`
- `infrastructure/database/sample-dev-data.sql`
- `infrastructure/keycloak/dev-realm.json`
- `infrastructure/weaviate/init-weaviate-dev.sh`
- `infrastructure/weaviate/dev-schema.json`

## Ports and URLs

| Service | URL or port |
| --- | --- |
| Frontend dev | `http://localhost:3000` |
| Backend dev | `http://localhost:8081` |
| Backend API | `http://localhost:8081/api` |
| Swagger UI | `http://localhost:8081/q/swagger-ui` |
| PostgreSQL | `localhost:5432` |
| Weaviate | `http://localhost:8080` |
| Keycloak | `http://localhost:8180` |
| Redis | `redis://localhost:6379` |
| Neo4j Browser | `http://localhost:7474` |
| Neo4j Bolt | `bolt://localhost:7687` |
| Ollama | `http://localhost:11434` |

## Backend native dev

Run:

```bash
cd backend && ./start-dev.sh
```

Important dev profile settings from `backend/src/main/resources/application-dev.properties`:

- Quarkus port `8081`
- OIDC realm `rag-app-dev`
- backend client `rag-app-backend`
- PostgreSQL database `rag_app_dev`
- Weaviate provider URL `http://localhost:8080`
- Ollama model `tinyllama`
- document storage path `./storage/documents`

Current backend toolchain notes:

- the workspace is aligned to Quarkus `3.32.3`
- Gradle `9.1.0` is used for Java 25 compatibility
- `backend/pom.xml` is kept aligned with the same Quarkus platform so Maven- and Gradle-based backend workflows do not diverge

Verification:

```bash
cd backend && ./test-dev-integration.sh
curl http://localhost:8081/q/health
```

## Frontend native dev

Run:

```bash
cd frontend && ./start-dev.sh
```

Useful environment values from `frontend/.env.development`:

- `REACT_APP_API_URL=http://localhost:8081/api`
- `REACT_APP_KEYCLOAK_URL=http://localhost:8180`
- `REACT_APP_KEYCLOAK_REALM=rag-app-dev`
- `REACT_APP_KEYCLOAK_CLIENT_ID=rag-app-frontend`
- `REACT_APP_SHOW_DEV_TOOLS=true`
- `REACT_APP_CHAT_TIMEOUT=20000`

## Authentication caveat

Keycloak realm configuration exists for backend and service integration, but the current frontend browser flow is still a dev stub.

Current frontend behavior in `frontend/src/config/keycloak.js`:

- debug mode auto-authenticates
- a fake token `dev-token` is used
- the default user is `jane.admin`
- the default role is `ADMIN`

That means frontend auth-related documentation should describe Keycloak service setup and test credentials without promising a full live browser login flow.

## Keycloak realm and dev users

Realm and clients:

- realm: `rag-app-dev`
- backend client: `rag-app-backend`
- frontend client: `rag-app-frontend`

Useful scripts:

```bash
./infrastructure/keycloak/validate-realm.sh
./infrastructure/keycloak/configure-dev-realm.sh
./infrastructure/keycloak/test-auth.sh
```

What changed for the current realm import flow:

- the dev realm JSON is aligned with Keycloak 23 import requirements
- client settings use supported Keycloak 23 attributes
- the compose service uses stronger health checks and verbose startup logging
- the validation script catches common compatibility mistakes before startup debugging begins

Common dev users:

- `john.doe / password123`
- `jane.admin / admin123`
- `test.user / test123`

## Environment template

Use `.env.dev.template` as the starting point for new local overrides.

Important values:

```properties
START_LLM=false
DB_PORT=5432
WEAVIATE_PORT=8080
KEYCLOAK_PORT=8180
REDIS_PORT=6379
NEO4J_HTTP_PORT=7474
NEO4J_BOLT_PORT=7687
OLLAMA_PORT=11434
```

When `START_LLM=false`, the core dev stack can still be considered healthy without Ollama.

## Database and vector tooling

Database scripts:

```bash
./infrastructure/database/status-dev-db.sh
./infrastructure/database/reset-dev-db.sh
./infrastructure/database/backup-dev-db.sh
```

Weaviate scripts:

```bash
./infrastructure/weaviate/init-weaviate-dev.sh
./infrastructure/weaviate/load-sample-data.sh
./infrastructure/weaviate/test-vector-search.sh
./infrastructure/weaviate/troubleshoot-weaviate.sh
./infrastructure/weaviate/manage-weaviate-dev.sh status
```

Important caveat: the Weaviate sample vectors are smoke-test data and use fake 10-dimensional vectors, while backend dev config declares `app.vector.dimension=384`.

Current Weaviate bootstrap behavior:

- the dev schema uses a simplified `DocumentChunk` class for startup reliability
- `./start-dev-services.sh` waits for Weaviate readiness before running schema initialization
- the initializer validates schema JSON before posting it
- if schema creation returns `422`, the initializer falls back to a minimal schema and prints recovery guidance

Neo4j scripts:

```bash
./infrastructure/neo4j/init-neo4j-dev.sh
./infrastructure/neo4j/load-sample-graph.sh
./infrastructure/neo4j/troubleshoot-neo4j.sh
```

Default local Neo4j development credentials:

- username: `neo4j`
- password: `dev-password`
