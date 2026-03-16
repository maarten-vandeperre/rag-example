## Summary
Updated the compose definitions for Podman Compose compatibility by removing dev-only fixed container names, adding SELinux-friendly bind mount relabeling, and naming shared networks and volumes consistently.

## Changes
- `docker-compose.yml`
- `docker-compose.dev.yml`

## Impact
Development and production compose workflows now behave more predictably under Podman Compose, avoid fixed-name conflicts, and work better on SELinux-enabled hosts with persistent Podman-managed resources.

## Verification
- `podman-compose -f "docker-compose.yml" config`
- `podman-compose -f "docker-compose.dev.yml" config`
- `podman-compose -p ragdevverify -f "docker-compose.dev.yml" up -d postgres-dev redis-dev`
- `podman-compose -p ragprodverify -f "docker-compose.yml" up -d postgres`

## Follow-ups
- Update service management scripts and docs to default to Podman Compose terminology.
- Extend runtime verification to the full service stack once image pull/build time is acceptable for local validation.
