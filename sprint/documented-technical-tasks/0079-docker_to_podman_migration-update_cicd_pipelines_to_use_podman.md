# Update CI/CD pipelines to use Podman

## Related User Story

Docker to Podman Migration

## Objective

Update all CI/CD pipeline configurations to use Podman instead of Docker for container operations, ensuring consistent container runtime across development and production environments.

## Scope

- Update GitHub Actions workflows to use Podman
- Update any other CI/CD pipeline configurations
- Update container build and push operations in pipelines
- Update integration test pipelines that use containers
- Verify all pipeline stages work with Podman

## Out of Scope

- Changing application deployment strategies
- Modifying container registry configurations
- Updating production runtime environments (separate consideration)

## Clean Architecture Placement

- infrastructure (CI/CD pipelines)

## Execution Dependencies

- 0078-docker_to_podman_migration-update_gradle_build_files_to_use_podman_for_container_operations.md

## Implementation Details

### GitHub Actions Workflow Updates
- Update workflow files to install and configure Podman
- Replace Docker commands with Podman commands in workflow steps
- Update container build and push actions to use Podman
- Verify workflow runners support Podman operations

### Container Build Pipeline Updates
- Update image build steps to use `podman build`
- Update image tagging and registry push operations
- Verify multi-stage builds work in CI environment
- Update any custom build scripts called from pipelines

### Integration Test Pipeline Updates
- Update test stages that use containers to use Podman
- Replace Docker-based test services with Podman equivalents
- Update test cleanup and teardown steps
- Verify test isolation works correctly with Podman

### Pipeline Environment Configuration
- Update environment variables and secrets for Podman
- Configure Podman registry authentication
- Update any pipeline caching strategies for Podman
- Verify pipeline performance with Podman operations

### Pipeline Validation
- Test all pipeline stages with Podman
- Verify parallel job execution works correctly
- Test pipeline failure scenarios and cleanup
- Validate container image artifacts are correctly produced

## Files / Modules Impacted

- `.github/workflows/` directory files
- Any other CI/CD configuration files
- Pipeline scripts and build automation
- Environment configuration files

## Acceptance Criteria

**Given** the updated CI/CD pipelines
**When** a pull request triggers the build pipeline
**Then** all container operations should complete successfully using Podman

**Given** the updated integration test pipeline
**When** tests run in the CI environment
**Then** all container-based tests should pass using Podman

**Given** the updated build pipeline
**When** container images are built and pushed
**Then** images should be successfully created and uploaded using Podman

**Given** multiple pipeline jobs running in parallel
**When** they perform container operations
**Then** Podman should handle concurrent operations without conflicts

## Testing Requirements

- Test all pipeline stages individually with Podman
- Test full pipeline execution end-to-end
- Test pipeline failure scenarios and recovery
- Test parallel pipeline execution
- Verify container image quality and functionality
- Test pipeline performance compared to Docker baseline

## Dependencies / Preconditions

- CI/CD runners support Podman installation and execution
- Container registries are accessible from Podman
- All Gradle build files have been updated for Podman
- Pipeline secrets and authentication are configured for Podman