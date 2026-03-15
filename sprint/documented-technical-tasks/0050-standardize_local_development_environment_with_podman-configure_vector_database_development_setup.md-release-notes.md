## Summary
Expanded the local Weaviate development setup with a richer schema, dev initialization and sample-data scripts, search/management utilities, and backend development vector-store configuration.

## Changes
Updated `infrastructure/weaviate/dev-schema.json` to define both `DocumentChunk` and `UserQuery` classes with development-oriented indexing properties.
Updated `infrastructure/weaviate/init-weaviate.sh` and `docker-compose.dev.yml` so the dev services initialize from the new development schema file.
Added `infrastructure/weaviate/init-weaviate-dev.sh`, `infrastructure/weaviate/load-sample-data.sh`, `infrastructure/weaviate/test-vector-search.sh`, and `infrastructure/weaviate/manage-weaviate-dev.sh` for local schema/data management.
Updated `backend/src/main/resources/application-dev.properties` and `README.md` with development vector-store configuration and operational commands.

## Impact
Developers now have a concrete local semantic-search setup for Weaviate that supports schema bootstrap, sample vector content, smoke testing, and repeatable operational workflows.

## Verification
Executed `chmod +x infrastructure/weaviate/init-weaviate-dev.sh infrastructure/weaviate/load-sample-data.sh infrastructure/weaviate/test-vector-search.sh infrastructure/weaviate/manage-weaviate-dev.sh`.
Executed `bash -n infrastructure/weaviate/init-weaviate-dev.sh infrastructure/weaviate/load-sample-data.sh infrastructure/weaviate/test-vector-search.sh infrastructure/weaviate/manage-weaviate-dev.sh`.
Executed `ruby -e 'require "yaml"; YAML.load_file("docker-compose.dev.yml")'`.
Executed `python3 -c 'import json; json.load(open("infrastructure/weaviate/dev-schema.json"))'`.
Executed `./gradlew --no-daemon healthCheck test`.

## Follow-ups
Run the new Weaviate scripts against a live local container to seed sample vectors and verify GraphQL queries end to end once the Podman services are up.
