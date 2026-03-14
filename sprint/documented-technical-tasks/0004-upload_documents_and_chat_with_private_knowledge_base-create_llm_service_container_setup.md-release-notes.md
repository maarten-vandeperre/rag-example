## Summary
Added an Ollama-based local LLM setup with automated model initialization, persisted model storage, and compose-level runtime configuration.

## Changes
- `docker-compose.yml`
- `infrastructure/ollama/init-ollama.sh`
- `infrastructure/ollama/docker-entrypoint.sh`
- `.env.example`
- `.env.development`

## Impact
Developers can now start a local Ollama service, auto-pull the default `tinyllama` model, call the generation API over the compose network, and retain downloaded models after container restarts.

## Verification
- `docker compose --env-file .env.development config`
- `podman-compose --env-file .env.development config`
- `podman-compose --env-file .env.development up -d ollama ollama-init`
- `podman wait <ollama-init-container>`
- `curlimages/curl` requests against `/api/tags` and `/api/generate`
- `podman restart rag-example_ollama_1`
- `podman exec rag-example_ollama_1 /bin/ollama list`
- `podman-compose --env-file .env.development down`

## Follow-ups
Wire the backend answer-generation flow to the Ollama endpoint and tune model/runtime defaults once the application code is in place.
