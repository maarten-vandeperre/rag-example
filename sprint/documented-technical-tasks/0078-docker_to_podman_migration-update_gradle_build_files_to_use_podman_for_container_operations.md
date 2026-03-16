# Update Gradle build files to use Podman for container operations

## Related User Story

Docker to Podman Migration

## Objective

Update all Gradle build files and tasks to use Podman instead of Docker for container build, test, and deployment operations.

## Scope

- Update Gradle tasks that use Docker commands to use Podman
- Update container build tasks in build.gradle files
- Update integration test tasks that use containers
- Update any Docker plugin configurations to work with Podman
- Verify all Gradle container operations work with Podman

## Out of Scope

- Changing Java application code
- Modifying non-container related Gradle tasks
- Updating CI/CD pipeline configurations (separate task)

## Clean Architecture Placement

- infrastructure (build system)

## Execution Dependencies

- 0077-docker_to_podman_migration-update_build_scripts_and_documentation_to_use_podman.md

## Implementation Details

### Gradle Task Updates
- Find all Gradle tasks that execute Docker commands
- Replace `docker` with `podman` in exec tasks
- Update any Docker-specific command line arguments for Podman
- Verify task dependencies and execution order still work

### Container Build Tasks
- Update tasks that build container images to use `podman build`
- Update image tagging and registry operations
- Verify multi-stage builds work correctly with Podman
- Update any custom build contexts or build arguments

### Integration Test Updates
- Update Gradle tasks that start test containers
- Replace Docker Testcontainers with Podman equivalents where possible
- Update test cleanup tasks to use Podman
- Verify test isolation and cleanup work correctly

### Plugin Configuration Updates
- Review Gradle plugins that interact with Docker
- Update plugin configurations to work with Podman
- Replace Docker-specific plugins with Podman-compatible alternatives
- Update any custom plugin configurations

### Build Script Verification
- Test all container-related Gradle tasks with Podman
- Verify build reproducibility with Podman
- Test parallel task execution with Podman
- Ensure proper error handling and cleanup

## Files / Modules Impacted

- Root `build.gradle`
- `backend/build.gradle`
- `frontend/build.gradle` (if applicable)
- Module-specific `build.gradle` files
- `gradle.properties` (if container-related properties exist)
- Any custom Gradle plugins or scripts

## Acceptance Criteria

**Given** the updated Gradle build files
**When** running container build tasks
**Then** images should be built successfully using Podman

**Given** the updated Gradle integration tests
**When** running tests that require containers
**Then** tests should pass using Podman-managed containers

**Given** the updated Gradle tasks
**When** running the full build pipeline
**Then** all container operations should complete successfully with Podman

**Given** parallel Gradle task execution
**When** multiple container operations run simultaneously
**Then** Podman should handle concurrent operations correctly

## Testing Requirements

- Test all container-related Gradle tasks individually
- Test full build pipeline with container operations
- Test integration tests that use containers
- Test parallel task execution
- Test error scenarios and cleanup behavior
- Verify build reproducibility across different environments

## Dependencies / Preconditions

- Podman is installed and accessible from Gradle build environment
- All shell scripts and documentation have been updated to use Podman
- Container images are available in Podman
- Gradle build system is functional