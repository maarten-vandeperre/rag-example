## Summary
Updated local service scripts and developer documentation to use Podman and Podman Compose as the primary container workflow across the workspace.

## Changes
- `start-dev-services.sh`
- `stop-dev-services.sh`
- `status-dev-services.sh`
- `troubleshoot-dev-services.sh`
- `infrastructure/neo4j/init-neo4j-dev.sh`
- `infrastructure/neo4j/load-sample-graph.sh`
- `infrastructure/neo4j/troubleshoot-neo4j.sh`
- `infrastructure/weaviate/init-weaviate-dev.sh`
- `infrastructure/weaviate/troubleshoot-weaviate.sh`
- `README.md`
- `docs/development/service-management.md`
- `docs/development/troubleshooting.md`
- `documentation/getting-started.md`
- `documentation/development-environment.md`
- `documentation/troubleshooting.md`
- `documentation/build-and-release.md`

## Impact
Developers now get Podman-first lifecycle scripts, cleaner local service management behavior, and updated onboarding/troubleshooting guidance that aligns with the Podman Compose-based environment.

## Verification
- `bash -n start-dev-services.sh stop-dev-services.sh status-dev-services.sh troubleshoot-dev-services.sh infrastructure/neo4j/init-neo4j-dev.sh infrastructure/neo4j/load-sample-graph.sh infrastructure/neo4j/troubleshoot-neo4j.sh infrastructure/weaviate/init-weaviate-dev.sh infrastructure/weaviate/troubleshoot-weaviate.sh`
- `./status-dev-services.sh && ./stop-dev-services.sh`
- `./gradlew healthCheck`

## Follow-ups
- Update remaining future migration tasks and historical docs that still mention Docker-specific commands where they become user-facing.
- Add dedicated Podman-based smoke tests for the full local stack lifecycle once startup time is acceptable for automation.
