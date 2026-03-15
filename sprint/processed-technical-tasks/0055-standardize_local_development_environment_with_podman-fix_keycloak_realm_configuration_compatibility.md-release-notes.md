## Summary
Updated the local Keycloak realm and service setup for better Keycloak 23 compatibility, including safer realm fields, stronger validation, improved auth checks, and more explicit dev-compose startup behavior.

## Changes
Updated `infrastructure/keycloak/dev-realm.json` to remove incompatible client fields and align realm/client settings with Keycloak 23 import expectations.
Updated `infrastructure/keycloak/configure-dev-realm.sh` and `infrastructure/keycloak/test-auth.sh` with validation, retries, readiness checks, and clearer error handling.
Added `infrastructure/keycloak/validate-realm.sh` for standalone realm compatibility validation.
Updated `docker-compose.dev.yml` Keycloak configuration with disabled features, verbose startup, and a stronger health check.

## Impact
Local Keycloak startup and realm import are less brittle, and developers now have a direct way to validate and troubleshoot realm compatibility before relying on the development auth flow.

## Verification
Executed `chmod +x infrastructure/keycloak/configure-dev-realm.sh infrastructure/keycloak/test-auth.sh infrastructure/keycloak/validate-realm.sh`.
Executed `bash -n infrastructure/keycloak/configure-dev-realm.sh infrastructure/keycloak/test-auth.sh infrastructure/keycloak/validate-realm.sh`.
Executed `./infrastructure/keycloak/validate-realm.sh`.
Executed `ruby -e 'require "yaml"; YAML.load_file("docker-compose.dev.yml")'`.
Executed `./gradlew --no-daemon healthCheck test`.

## Follow-ups
Run the updated Keycloak container and execute the auth smoke test against a live realm import to confirm the new compatibility changes behave correctly end to end.
