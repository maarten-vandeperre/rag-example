## Summary
Added a Podman-compatible compose stack for the RAG application with shared networking, persistent volumes, and development environment defaults.

## Changes
- `docker-compose.yml`
- `.env.example`
- `.env.development`

## Impact
Developers can validate and run a consistent local multi-service setup for PostgreSQL with pgvector, Weaviate, Ollama, backend, and frontend containers.

## Verification
- `docker compose --env-file .env.development config`
- `podman-compose --env-file .env.development config`

## Follow-ups
Add the backend, frontend, database, and infrastructure container assets referenced by the compose stack in subsequent technical tasks.
