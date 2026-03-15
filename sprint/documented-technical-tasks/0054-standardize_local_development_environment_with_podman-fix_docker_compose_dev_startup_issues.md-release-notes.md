## Summary
Stabilized the local development services stack by rewriting the dev compose file, making the lifecycle scripts container-runtime agnostic, and adding diagnostics plus an environment template for faster recovery from startup issues.

## Changes
Updated `docker-compose.dev.yml` with healthier PostgreSQL, Weaviate, Keycloak, Redis, and optional Ollama defaults plus better startup dependencies.
Updated `start-dev-services.sh`, `stop-dev-services.sh`, and `status-dev-services.sh`; added `troubleshoot-dev-services.sh` and `.env.dev.template`.

## Impact
Developers can start and inspect the local support services more reliably with Podman or Docker, get clearer failure output, and bootstrap Weaviate schema plus Keycloak import in a predictable order.

## Verification
Executed `chmod +x start-dev-services.sh stop-dev-services.sh status-dev-services.sh troubleshoot-dev-services.sh && bash -n start-dev-services.sh stop-dev-services.sh status-dev-services.sh troubleshoot-dev-services.sh && ruby -e 'require "yaml"; YAML.load_file("docker-compose.dev.yml")'`.
Executed `./gradlew test`.
Executed `bash -n start-dev-services.sh stop-dev-services.sh status-dev-services.sh troubleshoot-dev-services.sh && ruby -e 'require "yaml"; YAML.load_file("docker-compose.dev.yml")' && ./gradlew test`.

## Follow-ups
Consider adding a lightweight automated smoke test that runs the service scripts against a disposable container runtime in CI to catch future compose regressions earlier.
