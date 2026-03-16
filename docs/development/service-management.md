# Service Management Guide

The local service scripts support `podman-compose`, `docker-compose`, and `docker compose`.

## Lifecycle Scripts

```bash
./start-dev-services.sh
./status-dev-services.sh
./troubleshoot-dev-services.sh
./stop-dev-services.sh
./stop-dev-services.sh --clean
```

Behavior:

- `start-dev-services.sh` validates required bootstrap files before startup
- services start in dependency order: PostgreSQL and Redis, then Weaviate, schema init, Keycloak, and optional Ollama
- `troubleshoot-dev-services.sh` reports ports, recent logs, and connectivity
- `--clean` removes persisted volumes when local state is corrupted

## Supporting Services

### PostgreSQL

- Container: `rag-postgres-dev`
- Port: `5432`

```bash
podman logs rag-postgres-dev
./infrastructure/database/status-dev-db.sh
./infrastructure/database/backup-dev-db.sh
```

### Weaviate

- Container: `rag-weaviate-dev`
- Port: `8080`

```bash
curl http://localhost:8080/v1/meta
./infrastructure/weaviate/manage-weaviate-dev.sh status
./infrastructure/weaviate/manage-weaviate-dev.sh backup
```

### Keycloak

- Container: `rag-keycloak-dev`
- Port: `8180`
- Backed by PostgreSQL in the dev compose stack

```bash
curl http://localhost:8180/health/ready
./infrastructure/keycloak/test-auth.sh
```

### Redis

- Container: `rag-redis-dev`
- Port: `6379`

```bash
redis-cli -h localhost ping
```

### Neo4j

- Container: `rag-neo4j-dev`
- HTTP Browser Port: `7474`
- Bolt Port: `7687`

```bash
curl http://localhost:7474
./infrastructure/neo4j/init-neo4j-dev.sh
./infrastructure/neo4j/load-sample-graph.sh
./infrastructure/neo4j/troubleshoot-neo4j.sh
```

### Ollama

- Container: `rag-ollama-dev`
- Port: `11434`
- Optional: starts only when `START_LLM=true`

```bash
curl http://localhost:11434/api/tags
```

## Environment Template

Use `.env.dev.template` when creating a new `.env.dev` file.

Important toggles:

```properties
START_LLM=false
DB_PORT=5432
WEAVIATE_PORT=8080
KEYCLOAK_PORT=8180
REDIS_PORT=6379
OLLAMA_PORT=11434
```

## Native Apps

### Backend

```bash
cd backend && ./start-dev.sh
cd backend && ./gradlew devDebug
```

### Frontend

```bash
cd frontend && ./start-dev.sh
cd frontend && npm run test:dev
```
