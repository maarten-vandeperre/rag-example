# Add Neo4j to Podman Development Environment

## Related User Story

User Story: Enhance development environment with graph database capabilities

## Objective

Add Neo4j graph database to the existing Podman-based development environment to support graph-based data storage and querying capabilities for the RAG application.

## Scope

- Add Neo4j service to docker-compose.dev.yml
- Configure Neo4j with appropriate development settings
- Create initialization scripts for Neo4j schema/constraints
- Add health checks and monitoring
- Update development startup scripts
- Provide connection configuration for backend integration
- Add troubleshooting documentation

## Out of Scope

- Production Neo4j configuration
- Neo4j clustering setup
- Advanced security configurations
- Data migration from existing databases
- Backend code integration (separate task)

## Clean Architecture Placement

infrastructure

## Execution Dependencies

None

## Implementation Details

### Neo4j Service Configuration

Add Neo4j service to the existing Podman development environment with the following requirements:

**Service Configuration:**
- Use Neo4j Community Edition (latest stable version)
- Configure for development use with appropriate memory settings
- Enable Neo4j Browser for development/debugging
- Set up persistent data storage
- Configure authentication for development

**Network Integration:**
- Integrate with existing `rag-dev-network`
- Expose appropriate ports for browser and bolt protocol
- Ensure no port conflicts with existing services

**Data Persistence:**
- Create named volume for Neo4j data persistence
- Ensure data survives container restarts
- Configure appropriate file permissions

**Development Settings:**
- Enable development-friendly logging
- Configure memory settings appropriate for development
- Set up default database and user

### Port Allocation

Based on existing services in docker-compose.dev.yml:
- PostgreSQL: 5432
- Redis: 6379  
- Weaviate: 8080
- Keycloak: 8180
- Backend: 8081 (when running)
- Frontend: 3000 (when running)

**Proposed Neo4j Ports:**
- HTTP (Neo4j Browser): 7474
- Bolt Protocol: 7687

### Configuration Details

**Environment Variables:**
```yaml
NEO4J_AUTH: neo4j/dev-password
NEO4J_PLUGINS: ["apoc"]
NEO4J_dbms_security_procedures_unrestricted: apoc.*
NEO4J_dbms_memory_heap_initial_size: 512m
NEO4J_dbms_memory_heap_max_size: 1G
NEO4J_dbms_memory_pagecache_size: 256m
```

**Health Check:**
- Use Neo4j's built-in health endpoint
- Configure appropriate timeout and retry settings
- Ensure service is ready before dependent services start

**Initialization:**
- Create initialization script for development schema
- Set up basic constraints and indexes
- Provide sample data loading capability

## Files / Modules Impacted

- `docker-compose.dev.yml` - Add Neo4j service configuration
- `start-dev-services.sh` - Update startup script to include Neo4j
- `stop-dev-services.sh` - Update stop script to include Neo4j
- `troubleshoot-dev-services.sh` - Add Neo4j troubleshooting
- `infrastructure/neo4j/` - New directory for Neo4j configuration
  - `init-neo4j-dev.sh` - Neo4j initialization script
  - `dev-constraints.cypher` - Development constraints and indexes
  - `troubleshoot-neo4j.sh` - Neo4j-specific troubleshooting
- `README.md` or development documentation - Update with Neo4j information

## Acceptance Criteria

**Given** the Podman development environment is set up
**When** I run `./start-dev-services.sh`
**Then** Neo4j should start successfully alongside other services
**And** Neo4j Browser should be accessible at `http://localhost:7474`
**And** Bolt connection should be available at `bolt://localhost:7687`
**And** Health checks should pass for Neo4j service

**Given** Neo4j is running in the development environment
**When** I access the Neo4j Browser at `http://localhost:7474`
**Then** I should be able to log in with development credentials
**And** I should see an empty database ready for development
**And** Basic constraints and indexes should be pre-configured

**Given** the development environment is stopped
**When** I run `./stop-dev-services.sh`
**Then** Neo4j should stop gracefully
**And** Data should be persisted in the named volume

**Given** there are issues with Neo4j
**When** I run the troubleshooting script
**Then** I should get clear diagnostic information
**And** Common issues should be identified with solutions

## Testing Requirements

- Test Neo4j service startup and shutdown
- Verify Neo4j Browser accessibility and authentication
- Test Bolt protocol connectivity
- Verify data persistence across container restarts
- Test health check functionality
- Validate troubleshooting scripts provide useful information
- Ensure no port conflicts with existing services
- Test integration with existing development workflow

## Dependencies / Preconditions

- Existing Podman development environment must be functional
- Docker-compose.dev.yml must be working with current services
- Ports 7474 and 7687 must be available on the development machine
- Sufficient system resources for additional database service

## Implementation Notes

### Service Integration Strategy

1. **Incremental Addition**: Add Neo4j as an optional service initially
2. **Resource Management**: Configure appropriate memory limits for development
3. **Startup Order**: Ensure Neo4j starts independently of other services
4. **Error Handling**: Provide clear error messages for common issues

### Development Workflow Integration

1. **Documentation**: Update development setup documentation
2. **Scripts**: Ensure all existing scripts work with Neo4j addition
3. **Troubleshooting**: Provide comprehensive troubleshooting guidance
4. **Performance**: Monitor impact on overall development environment performance

### Security Considerations

1. **Development Only**: Use simple authentication suitable for development
2. **Network Isolation**: Ensure Neo4j is only accessible from development network
3. **Default Credentials**: Use well-known development credentials
4. **Plugin Security**: Configure APOC plugin with appropriate restrictions

### Future Extensibility

1. **Plugin Support**: Configure for easy addition of Neo4j plugins
2. **Multiple Databases**: Prepare for potential multi-database scenarios
3. **Backup Integration**: Consider future backup/restore capabilities
4. **Monitoring**: Prepare for potential monitoring integration