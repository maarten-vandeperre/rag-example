## Summary
Added a Weaviate container setup with schema bootstrapping scripts and compose wiring for local vector storage in the RAG stack.

## Changes
- `docker-compose.yml`
- `infrastructure/weaviate/weaviate-schema.json`
- `infrastructure/weaviate/init-weaviate.sh`
- `infrastructure/weaviate/docker-entrypoint.sh`
- `.env.example`
- `.env.development`

## Impact
Developers can now start Weaviate with persistent storage, initialize the `DocumentChunk` schema automatically, and keep vector objects available after container restarts.

## Verification
- `docker compose --env-file .env.development config`
- `podman-compose --env-file .env.development config`
- `podman-compose --env-file .env.development up -d weaviate weaviate-init`
- `podman wait <weaviate-init-container>`
- `podman run --rm --network rag-example_rag-network curlimages/curl:8.12.1 ...`
- `podman restart <weaviate-container>`
- `podman-compose --env-file .env.development down`

## Follow-ups
Add application-side vector ingestion and query flows that write embeddings into the `DocumentChunk` class.
