## Summary
Fixed the local Weaviate initialization flow to better handle schema startup issues, reduce 422 failure cases, and surface clearer diagnostics during development-service startup.

## Changes
Updated `infrastructure/weaviate/dev-schema.json` to use the simplified single-class development schema expected by the startup flow.
Updated `infrastructure/weaviate/init-weaviate-dev.sh` with stricter shell safety, schema validation, existing-schema handling, and a clearer 422 fallback path.
Updated `start-dev-services.sh` and `infrastructure/weaviate/troubleshoot-weaviate.sh` to provide better recovery guidance when Weaviate initialization fails.
Updated `docker-compose.dev.yml` Weaviate settings for the simplified dev setup.
Updated `build.gradle` and `gradle/wrapper/gradle-wrapper.properties` to align the workspace with Quarkus 3.32.3 on Gradle 9.1.0 so repository verification succeeds on Java 25.

## Impact
Weaviate schema bootstrapping is now more resilient during local startup, and the repository build is compatible with the current Java 25 and Quarkus toolchain used by the workspace.

## Verification
Executed `chmod +x infrastructure/weaviate/init-weaviate-dev.sh infrastructure/weaviate/troubleshoot-weaviate.sh`.
Executed `bash -n infrastructure/weaviate/init-weaviate-dev.sh infrastructure/weaviate/troubleshoot-weaviate.sh start-dev-services.sh`.
Executed `python3 -c 'import json; json.load(open("infrastructure/weaviate/dev-schema.json"))'`.
Executed `ruby -e 'require "yaml"; YAML.load_file("docker-compose.dev.yml")'`.
Executed `./gradlew --no-daemon healthCheck test`.

## Follow-ups
Run the updated Weaviate startup flow against live local containers and consider cleaning up the remaining Gradle 9 deprecation warnings in `backend/build.gradle` next.
