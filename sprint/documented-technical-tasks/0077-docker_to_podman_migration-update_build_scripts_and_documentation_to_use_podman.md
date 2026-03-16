# Update build scripts and documentation to use Podman

## Related User Story

Docker to Podman Migration

## Objective

Update all build scripts, development scripts, and documentation to use Podman commands instead of Docker commands throughout the project.

## Scope

- Update shell scripts to use `podman` instead of `docker`
- Update build scripts to use `podman build` instead of `docker build`
- Update documentation and README files
- Update development setup instructions
- Update any Makefile or task runner configurations

## Out of Scope

- Changing container images or Dockerfiles
- Updating CI/CD pipelines (separate task)
- Modifying application code

## Clean Architecture Placement

- infrastructure (build and deployment scripts)
- documentation

## Execution Dependencies

- 0076-docker_to_podman_migration-update_docker_compose_files_to_use_podman_compose.md

## Implementation Details

### Shell Script Updates
- Find all shell scripts that use `docker` commands
- Replace `docker` with `podman` in all scripts
- Replace `docker-compose` with `podman-compose`
- Update any Docker-specific flags that differ in Podman
- Test all scripts to ensure they work with Podman

### Build Script Updates
- Update container build commands to use `podman build`
- Update image tagging and pushing commands
- Verify build contexts work correctly with Podman
- Update any multi-stage build processes

### Documentation Updates
- Update README.md files to reference Podman
- Update development setup instructions
- Update deployment documentation
- Update troubleshooting guides
- Add notes about Podman-specific considerations

### Development Environment Scripts
- Update start-dev-services.sh to use Podman
- Update stop-dev-services.sh to use Podman
- Update any cleanup or reset scripts
- Update integration test scripts

### Configuration File Updates
- Update any Makefile targets that use Docker
- Update package.json scripts if they reference Docker
- Update any IDE configurations that reference Docker
- Update environment variable documentation

## Files / Modules Impacted

- `start-dev-services.sh`
- `stop-dev-services.sh`
- `README.md`
- `docs/` directory files
- Any `Makefile` files
- `package.json` (if applicable)
- Build and deployment scripts
- Integration test scripts

## Acceptance Criteria

**Given** the updated scripts
**When** running development setup commands
**Then** all services should start using Podman instead of Docker

**Given** the updated documentation
**When** a new developer follows the setup instructions
**Then** they should be able to set up the environment using only Podman

**Given** the updated build scripts
**When** building container images
**Then** images should be built successfully using Podman

**Given** all scripts are updated
**When** searching the codebase for "docker" references
**Then** only legitimate references (like Dockerfile names) should remain

## Testing Requirements

- Test all updated scripts with Podman
- Verify development environment setup works end-to-end
- Test container build processes
- Verify integration test scripts work correctly
- Test cleanup and reset scripts

## Dependencies / Preconditions

- Podman and Podman Compose are installed and working
- Docker Compose files have been updated for Podman compatibility
- All container images are available in Podman