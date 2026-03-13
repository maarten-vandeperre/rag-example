## Summary
Added a custom PostgreSQL 15 container with pgvector installed and initialization scripts for the RAG application schema and seed users.

## Changes
- `infrastructure/database/Dockerfile`
- `infrastructure/database/init.sql`
- `infrastructure/database/sample-data.sql`
- `docker-compose.yml`
- `.env.example`
- `.env.development`

## Impact
The local compose stack can now build a PostgreSQL service that provisions application tables, enables vector operations, and preserves data across restarts.

## Verification
- `docker compose --env-file .env.development config`
- `podman-compose --env-file .env.development config`
- `podman-compose --env-file .env.development build postgres`
- `podman-compose --env-file .env.development up -d postgres`
- `podman exec <postgres-container> psql ...` checks for extension, schema, vector query, sample users, and restart persistence

## Follow-ups
Connect upcoming backend persistence code to the seeded schema and align future vector-store work with the `document_chunks` model.
