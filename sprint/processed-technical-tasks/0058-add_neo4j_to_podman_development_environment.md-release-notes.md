## Summary
Added Neo4j to the Podman-based development stack with local service orchestration, schema bootstrap scripts, sample graph data, troubleshooting helpers, and backend development connection settings.

## Changes
Updated `docker-compose.dev.yml`, `.env.dev`, `start-dev-services.sh`, `status-dev-services.sh`, and `troubleshoot-dev-services.sh` to include Neo4j service startup, health checks, ports, volumes, and diagnostics.
Added `infrastructure/neo4j/dev-constraints.cypher`, `infrastructure/neo4j/sample-dev-data.cypher`, `infrastructure/neo4j/init-neo4j-dev.sh`, `infrastructure/neo4j/load-sample-graph.sh`, and `infrastructure/neo4j/troubleshoot-neo4j.sh` for schema initialization and graph troubleshooting.
Updated `backend/src/main/resources/application-dev.properties`, `README.md`, and `docs/development/service-management.md` with Neo4j development connection details and operational guidance.
Updated backend upload tests to align with the existing processing-on-upload behavior so repository verification remains green.

## Impact
Developers can now run a local Neo4j graph database alongside the existing dev services and have a documented, scriptable workflow for initializing and inspecting graph data during development.

## Verification
Executed `chmod +x infrastructure/neo4j/init-neo4j-dev.sh infrastructure/neo4j/load-sample-graph.sh infrastructure/neo4j/troubleshoot-neo4j.sh`.
Executed `bash -n start-dev-services.sh status-dev-services.sh troubleshoot-dev-services.sh infrastructure/neo4j/init-neo4j-dev.sh infrastructure/neo4j/load-sample-graph.sh infrastructure/neo4j/troubleshoot-neo4j.sh`.
Executed `ruby -e 'require "yaml"; YAML.load_file("docker-compose.dev.yml")'`.
Executed `./gradlew --no-daemon healthCheck test`.

## Follow-ups
Hook the new Neo4j development settings into backend graph-query code once the graph persistence feature work begins, and add end-to-end smoke tests against a live Neo4j container when those integrations exist.
