## Summary
Added a Podman-focused local development services stack for PostgreSQL, Weaviate, Keycloak, Redis, and Ollama, plus environment files, startup/status scripts, and supporting dev seed data.

## Changes
Added `docker-compose.dev.yml`, `.env.dev`, `start-dev-services.sh`, `stop-dev-services.sh`, and `status-dev-services.sh` for local supporting services orchestration.
Added development bootstrap data in `infrastructure/database/init-dev.sql`, `infrastructure/database/sample-dev-data.sql`, `infrastructure/keycloak/dev-realm.json`, and `infrastructure/weaviate/dev-schema.json`.
Updated `README.md` with the new development-services workflow.

## Impact
Developers can now boot only the local supporting infrastructure with Podman while running backend and frontend directly on the host, which matches the requested development workflow more closely.

## Verification
Executed `chmod +x start-dev-services.sh stop-dev-services.sh status-dev-services.sh`.
Executed `bash -n start-dev-services.sh stop-dev-services.sh status-dev-services.sh`.
Executed `ruby -e 'require "yaml"; YAML.load_file("docker-compose.dev.yml")'`.
Executed `python3 -c 'import json; json.load(open("infrastructure/keycloak/dev-realm.json")); json.load(open("infrastructure/weaviate/dev-schema.json"))'`.
Executed `./gradlew --no-daemon healthCheck test`.

## Follow-ups
Consider wiring the development Keycloak realm and service URLs into backend runtime defaults once authentication moves from placeholder behavior to real local identity integration.
