## Summary
Added a Podman-based GitHub Actions workflow so CI validates compose files, runs targeted tests, and builds container images with Podman instead of Docker-oriented pipeline steps.

## Changes
- `.github/workflows/podman-ci.yml`

## Impact
The repository now has CI/CD automation aligned with the Podman migration, giving pull requests and manual runs a consistent Podman toolchain for container validation and image creation.

## Verification
- `ruby -e 'require "yaml"; YAML.load_file(".github/workflows/podman-ci.yml")'`
- `./gradlew healthCheck`
- `./gradlew :backend:shared-kernel:test :frontend:test`
- `./gradlew verifyPodmanContainerWorkflow`

## Follow-ups
- Add branch protection and registry publishing steps once the deployment pipeline is ready for Podman-based release jobs.
- Extend CI coverage to full backend module tests after unrelated compile/test fixture drift is resolved.
