## Summary
Added a dedicated `docs/development/` documentation set covering local environment setup, daily workflows, troubleshooting, service operations, and onboarding for the Podman-based development stack.

## Changes
Added `docs/development/README.md` with the main local development overview and quick-start flow.
Added `docs/development/daily-workflows.md`, `docs/development/troubleshooting.md`, `docs/development/service-management.md`, and `docs/development/onboarding-checklist.md`.
Updated `README.md` to point developers to the new documentation hub.

## Impact
Developers now have a single place to learn, operate, troubleshoot, and onboard into the standardized local development environment without piecing together commands from individual task outputs.

## Verification
Executed `./gradlew --no-daemon healthCheck test`.

## Follow-ups
Keep the new docs aligned with future local-development script changes, especially if auth, service ports, or startup order changes again.
