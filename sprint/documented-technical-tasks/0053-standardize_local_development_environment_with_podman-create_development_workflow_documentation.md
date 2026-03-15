# Create Development Workflow Documentation

## Related User Story

User Story: standardize_local_development_environment_with_podman

## Objective

Create comprehensive documentation for the standardized local development environment, including setup instructions, daily workflows, troubleshooting guides, and onboarding materials for new developers.

## Scope

- Create developer onboarding guide
- Document daily development workflows
- Create troubleshooting and FAQ documentation
- Document service management procedures
- Create quick reference guides
- Add architecture overview for development environment

## Out of Scope

- Production deployment documentation
- Advanced system administration guides
- Performance tuning documentation
- Security hardening guides

## Clean Architecture Placement

documentation

## Execution Dependencies

- 0047-standardize_local_development_environment_with_podman-create_development_services_compose.md
- 0051-standardize_local_development_environment_with_podman-configure_backend_development_integration.md
- 0052-standardize_local_development_environment_with_podman-configure_frontend_development_integration.md

## Implementation Details

Create main development documentation (docs/development/README.md):
```markdown
# RAG Application Development Environment

This document provides comprehensive guidance for setting up and using the local development environment for the RAG (Retrieval-Augmented Generation) application.

## Overview

The development environment uses a hybrid approach:
- **Supporting services** run in Podman containers (PostgreSQL, Weaviate, Keycloak, Redis, Ollama)
- **Application code** runs natively for optimal development experience (Backend in Quarkus dev mode, Frontend in React dev mode)

## Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Frontend      │    │    Backend      │    │   Supporting    │
│  (React Dev)    │    │ (Quarkus Dev)   │    │   Services      │
│  Port: 3000     │    │  Port: 8081     │    │  (Containers)   │
│                 │    │                 │    │                 │
│ • Hot Reload    │◄──►│ • Live Reload   │◄──►│ • PostgreSQL    │
│ • Debug Tools   │    │ • Debug Port    │    │ • Weaviate      │
│ • Keycloak Auth │    │ • Health Checks │    │ • Keycloak      │
└─────────────────┘    └─────────────────┘    │ • Redis         │
                                              │ • Ollama        │
                                              └─────────────────┘
```

## Quick Start

### Prerequisites

- **Podman** and **podman-compose** installed
- **Java 25** installed and configured
- **Node.js 18+** installed
- **Git** for version control

### 1. Clone and Setup

```bash
git clone <repository-url>
cd rag-example

# Make scripts executable
chmod +x *.sh
chmod +x backend/*.sh
chmod +x frontend/*.sh
chmod +x infrastructure/**/*.sh
```

### 2. Start Supporting Services

```bash
# Start all supporting services (PostgreSQL, Weaviate, Keycloak, etc.)
./start-dev-services.sh

# Check status
./status-dev-services.sh
```

### 3. Start Backend

```bash
cd backend
./start-dev.sh
# Or alternatively: ./gradlew dev
```

### 4. Start Frontend

```bash
cd frontend
./start-dev.sh
# Or alternatively: npm run start:dev
```

### 5. Access the Application

- **Frontend**: http://localhost:3000
- **Backend API**: http://localhost:8081/api
- **API Documentation**: http://localhost:8081/q/swagger-ui
- **Keycloak Admin**: http://localhost:8180/admin (admin/admin123)

## Development Credentials

### Keycloak Users
- **john.doe** / password123 (STANDARD role)
- **jane.admin** / admin123 (ADMIN role)
- **test.user** / test123 (STANDARD role)

### Database Access
- **Host**: localhost:5432
- **Database**: rag_app_dev
- **Username**: rag_dev_user
- **Password**: rag_dev_password

## Daily Workflows

See [Daily Workflows](./daily-workflows.md) for detailed information.

## Troubleshooting

See [Troubleshooting Guide](./troubleshooting.md) for common issues and solutions.

## Service Management

See [Service Management](./service-management.md) for managing individual services.
```

Create daily workflows documentation (docs/development/daily-workflows.md):
```markdown
# Daily Development Workflows

## Starting Your Development Day

### 1. Check Service Status
```bash
# Check if services are running
./status-dev-services.sh

# If services are not running, start them
./start-dev-services.sh
```

### 2. Start Application Components
```bash
# Terminal 1: Start backend
cd backend && ./start-dev.sh

# Terminal 2: Start frontend
cd frontend && ./start-dev.sh
```

### 3. Verify Everything is Working
```bash
# Test backend integration
cd backend && ./test-dev-integration.sh

# Test frontend integration
cd frontend && ./test-dev-integration.sh
```

## Common Development Tasks

### Working with Documents
1. **Upload a test document**:
   - Go to http://localhost:3000
   - Login with john.doe/password123
   - Navigate to Documents
   - Upload a PDF, Markdown, or text file

2. **Check document processing**:
   - Monitor backend logs for processing status
   - Check database: `./infrastructure/database/status-dev-db.sh`
   - Check vector storage: `./infrastructure/weaviate/manage-weaviate-dev.sh status`

### Working with Chat
1. **Test chat functionality**:
   - Upload and process a document first
   - Navigate to Chat workspace
   - Ask questions about your document
   - Verify source references are included

### Database Operations
```bash
# Check database status
./infrastructure/database/status-dev-db.sh

# Reset database with fresh sample data
./infrastructure/database/reset-dev-db.sh

# Backup current database
./infrastructure/database/backup-dev-db.sh
```

### Vector Database Operations
```bash
# Check Weaviate status
./infrastructure/weaviate/manage-weaviate-dev.sh status

# Load sample vector data
./infrastructure/weaviate/manage-weaviate-dev.sh load-sample

# Test vector search
./infrastructure/weaviate/manage-weaviate-dev.sh test
```

### Authentication Testing
```bash
# Test Keycloak authentication
./infrastructure/keycloak/test-auth.sh

# Reset Keycloak realm (if needed)
./infrastructure/keycloak/configure-dev-realm.sh
```

## Code Changes and Hot Reload

### Backend Changes
- **Java code**: Automatically reloaded by Quarkus dev mode
- **Configuration**: Restart backend for application.properties changes
- **Database schema**: Run migration scripts manually

### Frontend Changes
- **React components**: Automatically reloaded by React dev server
- **Environment variables**: Restart frontend for .env changes
- **Dependencies**: Run `npm install` and restart

## Testing Your Changes

### Unit Tests
```bash
# Backend tests
cd backend && ./gradlew test

# Frontend tests
cd frontend && npm test
```

### Integration Tests
```bash
# Backend integration with services
cd backend && ./test-dev-integration.sh

# Frontend integration with backend
cd frontend && ./test-dev-integration.sh
```

### Manual Testing Checklist
- [ ] Document upload works
- [ ] Document processing completes
- [ ] Chat queries return answers with references
- [ ] User authentication works
- [ ] Admin functions work (if applicable)
- [ ] Error handling works properly

## Ending Your Development Day

### 1. Commit Your Changes
```bash
git add .
git commit -m "Your commit message"
git push
```

### 2. Stop Services (Optional)
```bash
# Stop application components (Ctrl+C in terminals)

# Stop supporting services (optional - they can run continuously)
./stop-dev-services.sh
```

## Performance Tips

### Faster Startup
- Keep supporting services running between sessions
- Use `./gradlew --daemon` for faster Gradle builds
- Use `npm ci` instead of `npm install` for faster dependency installation

### Resource Management
- Monitor memory usage: `docker stats` or `podman stats`
- Adjust JVM heap size if needed: `export JAVA_OPTS="-Xmx4g"`
- Close unused browser tabs to save memory
```

Create troubleshooting guide (docs/development/troubleshooting.md):
```markdown
# Troubleshooting Guide

## Common Issues and Solutions

### Services Won't Start

#### Problem: Podman services fail to start
```
Error: port 5432 already in use
```

**Solution**:
```bash
# Check what's using the port
sudo lsof -i :5432

# Stop conflicting services
sudo systemctl stop postgresql  # If system PostgreSQL is running

# Or use different ports in docker-compose.dev.yml
```

#### Problem: Insufficient resources
```
Error: cannot create container: insufficient memory
```

**Solution**:
```bash
# Check available resources
podman system df
podman system prune  # Clean up unused containers/images

# Increase Podman machine resources (if using Podman Desktop)
podman machine set --memory 8192 --cpus 4
```

### Backend Issues

#### Problem: Backend can't connect to database
```
ERROR: Connection refused to localhost:5432
```

**Solution**:
```bash
# Check if PostgreSQL container is running
podman ps | grep postgres

# Check PostgreSQL logs
podman logs rag-postgres-dev

# Test connection manually
pg_isready -h localhost -p 5432 -U rag_dev_user
```

#### Problem: Keycloak authentication fails
```
ERROR: Unable to verify JWT token
```

**Solution**:
```bash
# Check Keycloak status
curl http://localhost:8180/health/ready

# Verify realm configuration
./infrastructure/keycloak/test-auth.sh

# Reconfigure realm if needed
./infrastructure/keycloak/configure-dev-realm.sh
```

### Frontend Issues

#### Problem: CORS errors in browser
```
Access to fetch at 'http://localhost:8081/api' from origin 'http://localhost:3000' has been blocked by CORS policy
```

**Solution**:
1. Check backend CORS configuration in `application-dev.properties`
2. Verify backend is running on correct port (8081)
3. Clear browser cache and cookies

#### Problem: Keycloak authentication popup blocked
```
Popup blocked by browser
```

**Solution**:
1. Allow popups for localhost:3000
2. Or use redirect mode instead of popup mode in Keycloak config

### Vector Database Issues

#### Problem: Weaviate schema not found
```
ERROR: Class 'DocumentChunk' not found
```

**Solution**:
```bash
# Reinitialize Weaviate schema
./infrastructure/weaviate/init-weaviate-dev.sh

# Check schema status
./infrastructure/weaviate/manage-weaviate-dev.sh schema
```

#### Problem: Vector search returns no results
```
No similar documents found
```

**Solution**:
```bash
# Check if documents are indexed
./infrastructure/weaviate/manage-weaviate-dev.sh status

# Load sample data for testing
./infrastructure/weaviate/manage-weaviate-dev.sh load-sample
```

## Performance Issues

### Slow Startup

#### Problem: Services take too long to start
**Solution**:
- Increase timeout values in health checks
- Allocate more resources to Podman
- Use SSD storage for better I/O performance

### High Memory Usage

#### Problem: System running out of memory
**Solution**:
```bash
# Check memory usage
podman stats

# Stop unnecessary services
podman stop rag-redis-dev rag-ollama-dev  # If not needed

# Adjust JVM heap size
export JAVA_OPTS="-Xmx2g"
```

## Network Issues

### Port Conflicts

#### Problem: Port already in use
**Solution**:
```bash
# Find what's using the port
sudo netstat -tulpn | grep :8080

# Kill the process or change port in configuration
```

### DNS Resolution

#### Problem: Services can't reach each other
**Solution**:
```bash
# Check Podman network
podman network ls
podman network inspect rag-dev-network

# Restart networking
podman network rm rag-dev-network
./start-dev-services.sh
```

## Data Issues

### Database Problems

#### Problem: Database schema is corrupted
**Solution**:
```bash
# Reset database
./infrastructure/database/reset-dev-db.sh

# Or restore from backup
./infrastructure/database/backup-dev-db.sh  # If you have one
```

#### Problem: Sample data is missing
**Solution**:
```bash
# Reload sample data
./infrastructure/database/reset-dev-db.sh
```

### Vector Data Problems

#### Problem: Vector embeddings are missing
**Solution**:
```bash
# Reset Weaviate data
./infrastructure/weaviate/manage-weaviate-dev.sh reset

# Reload sample data
./infrastructure/weaviate/manage-weaviate-dev.sh load-sample
```

## Getting Help

### Debug Information Collection

When reporting issues, collect this information:

```bash
# System information
uname -a
podman version
java -version
node --version

# Service status
./status-dev-services.sh
podman ps -a
podman logs rag-postgres-dev
podman logs rag-weaviate-dev
podman logs rag-keycloak-dev

# Application logs
# Backend logs from terminal
# Frontend logs from browser console
```

### Escalation to Lead Architect

If issues persist after trying these solutions:

1. **Collect debug information** (see above)
2. **Document the exact error message**
3. **List the steps to reproduce**
4. **Include your environment details**
5. **Contact the lead architect** with all this information

### Reset Everything

As a last resort, reset the entire development environment:

```bash
# Stop all services
./stop-dev-services.sh

# Remove all containers and volumes
podman system prune -a --volumes

# Start fresh
./start-dev-services.sh
cd backend && ./start-dev.sh
cd frontend && ./start-dev.sh
```
```

Create service management guide (docs/development/service-management.md):
```markdown
# Service Management Guide

## Overview

The development environment consists of several containerized supporting services and native application components.

## Supporting Services (Containerized)

### PostgreSQL Database
- **Container**: rag-postgres-dev
- **Port**: 5432
- **Purpose**: Primary application database

#### Management Commands
```bash
# Check status
podman ps | grep postgres
podman logs rag-postgres-dev

# Connect to database
PGPASSWORD=rag_dev_password psql -h localhost -p 5432 -U rag_dev_user -d rag_app_dev

# Backup/Restore
./infrastructure/database/backup-dev-db.sh
./infrastructure/database/reset-dev-db.sh
```

### Weaviate Vector Database
- **Container**: rag-weaviate-dev
- **Port**: 8080
- **Purpose**: Vector storage and semantic search

#### Management Commands
```bash
# Check status
curl http://localhost:8080/v1/meta

# Manage data
./infrastructure/weaviate/manage-weaviate-dev.sh status
./infrastructure/weaviate/manage-weaviate-dev.sh reset
./infrastructure/weaviate/manage-weaviate-dev.sh backup
```

### Keycloak Authentication
- **Container**: rag-keycloak-dev
- **Port**: 8180
- **Purpose**: Authentication and authorization

#### Management Commands
```bash
# Check status
curl http://localhost:8180/health/ready

# Admin console
# URL: http://localhost:8180/admin
# Credentials: admin/admin123

# Test authentication
./infrastructure/keycloak/test-auth.sh
```

### Redis Cache
- **Container**: rag-redis-dev
- **Port**: 6379
- **Purpose**: Session storage and caching

#### Management Commands
```bash
# Check status
redis-cli -h localhost ping

# Connect to Redis
redis-cli -h localhost

# Monitor commands
redis-cli -h localhost monitor
```

### Ollama LLM Service
- **Container**: rag-ollama-dev
- **Port**: 11434
- **Purpose**: Local language model hosting

#### Management Commands
```bash
# Check status
curl http://localhost:11434/api/tags

# List models
curl http://localhost:11434/api/tags | jq '.models'

# Pull new model
curl -X POST http://localhost:11434/api/pull -d '{"name": "llama2:7b-chat"}'
```

## Application Components (Native)

### Backend (Quarkus)
- **Port**: 8081
- **Mode**: Development with hot reload

#### Management Commands
```bash
# Start
cd backend && ./start-dev.sh

# Start with debugging
cd backend && ./gradlew devDebug

# Health check
curl http://localhost:8081/q/health

# API documentation
# URL: http://localhost:8081/q/swagger-ui
```

### Frontend (React)
- **Port**: 3000
- **Mode**: Development with hot reload

#### Management Commands
```bash
# Start
cd frontend && ./start-dev.sh

# Install dependencies
cd frontend && npm install

# Run tests
cd frontend && npm test
```

## Service Dependencies

```
Frontend (3000) → Backend (8081) → PostgreSQL (5432)
                                 → Weaviate (8080)
                                 → Keycloak (8180)
                                 → Redis (6379)
                                 → Ollama (11434)
```

## Startup Order

1. **Supporting Services** (can start in parallel)
   ```bash
   ./start-dev-services.sh
   ```

2. **Backend** (after supporting services are ready)
   ```bash
   cd backend && ./start-dev.sh
   ```

3. **Frontend** (after backend is ready)
   ```bash
   cd frontend && ./start-dev.sh
   ```

## Monitoring and Logs

### Container Logs
```bash
# All containers
podman logs -f rag-postgres-dev
podman logs -f rag-weaviate-dev
podman logs -f rag-keycloak-dev

# Follow logs in real-time
podman logs -f --tail 100 rag-postgres-dev
```

### Application Logs
```bash
# Backend logs (in terminal where it's running)
# Frontend logs (in browser console)

# Health checks
curl http://localhost:8081/q/health
curl http://localhost:8080/v1/meta
curl http://localhost:8180/health/ready
```

### Resource Usage
```bash
# Container resource usage
podman stats

# System resource usage
htop
df -h
```

## Backup and Recovery

### Database Backup
```bash
# Create backup
./infrastructure/database/backup-dev-db.sh

# Restore from backup
PGPASSWORD=rag_dev_password psql -h localhost -p 5432 -U rag_dev_user -d rag_app_dev < backup_file.sql
```

### Vector Database Backup
```bash
# Create backup
./infrastructure/weaviate/manage-weaviate-dev.sh backup

# Reset and restore
./infrastructure/weaviate/manage-weaviate-dev.sh reset
# Then reload your data
```

### Configuration Backup
```bash
# Backup important configuration files
tar -czf dev-config-backup.tar.gz \
  docker-compose.dev.yml \
  .env.dev \
  infrastructure/ \
  backend/src/main/resources/application-dev.properties \
  frontend/.env.development
```

## Troubleshooting Service Issues

### Service Won't Start
1. Check port availability: `netstat -tulpn | grep :PORT`
2. Check Podman status: `podman ps -a`
3. Check logs: `podman logs CONTAINER_NAME`
4. Restart service: `podman restart CONTAINER_NAME`

### Service Performance Issues
1. Check resource usage: `podman stats`
2. Check system resources: `htop`, `df -h`
3. Adjust resource limits in docker-compose.dev.yml
4. Restart with more resources

### Network Connectivity Issues
1. Check network: `podman network inspect rag-dev-network`
2. Test connectivity: `podman exec CONTAINER ping OTHER_CONTAINER`
3. Restart network: `podman network rm rag-dev-network && ./start-dev-services.sh`
```

Create onboarding checklist (docs/development/onboarding-checklist.md):
```markdown
# Developer Onboarding Checklist

## Prerequisites Installation

### Required Software
- [ ] **Podman** and **podman-compose** installed
  - Installation guide: https://podman.io/getting-started/installation
  - Verify: `podman version` and `podman-compose version`

- [ ] **Java 25** installed and configured
  - Download from: https://jdk.java.net/25/
  - Verify: `java -version` shows version 25
  - Set JAVA_HOME environment variable

- [ ] **Node.js 18+** installed
  - Download from: https://nodejs.org/
  - Verify: `node --version` shows 18.x or higher
  - Verify: `npm --version`

- [ ] **Git** installed and configured
  - Verify: `git --version`
  - Configure: `git config --global user.name "Your Name"`
  - Configure: `git config --global user.email "your.email@example.com"`

### Optional Tools
- [ ] **PostgreSQL client tools** (for database access)
  - Install: `psql`, `pg_isready`
- [ ] **Redis CLI** (for cache debugging)
  - Install: `redis-cli`
- [ ] **curl** and **jq** (for API testing)
- [ ] **IDE/Editor** with Java and JavaScript support
  - Recommended: IntelliJ IDEA, VS Code

## Repository Setup

- [ ] **Clone the repository**
  ```bash
  git clone <repository-url>
  cd rag-example
  ```

- [ ] **Make scripts executable**
  ```bash
  chmod +x *.sh
  chmod +x backend/*.sh
  chmod +x frontend/*.sh
  chmod +x infrastructure/**/*.sh
  ```

- [ ] **Review project structure**
  - [ ] Read main README.md
  - [ ] Understand module organization
  - [ ] Review development documentation

## Environment Setup

- [ ] **Start supporting services**
  ```bash
  ./start-dev-services.sh
  ```

- [ ] **Verify services are running**
  ```bash
  ./status-dev-services.sh
  ```

- [ ] **Initialize development data**
  ```bash
  # Database should auto-initialize
  # Verify with: ./infrastructure/database/status-dev-db.sh
  
  # Initialize Weaviate schema
  ./infrastructure/weaviate/init-weaviate-dev.sh
  
  # Load sample vector data
  ./infrastructure/weaviate/load-sample-data.sh
  ```

## Application Setup

- [ ] **Start backend application**
  ```bash
  cd backend
  ./start-dev.sh
  ```

- [ ] **Verify backend is working**
  ```bash
  # In another terminal
  cd backend
  ./test-dev-integration.sh
  ```

- [ ] **Start frontend application**
  ```bash
  cd frontend
  ./start-dev.sh
  ```

- [ ] **Verify frontend is working**
  ```bash
  # In another terminal
  cd frontend
  ./test-dev-integration.sh
  ```

## First Application Test

- [ ] **Access the application**
  - [ ] Open http://localhost:3000 in browser
  - [ ] Verify the application loads

- [ ] **Test authentication**
  - [ ] Click login button
  - [ ] Login with: john.doe / password123
  - [ ] Verify successful authentication

- [ ] **Test document upload**
  - [ ] Navigate to Documents section
  - [ ] Upload a test PDF or text file
  - [ ] Verify upload succeeds and processing starts

- [ ] **Test chat functionality**
  - [ ] Wait for document processing to complete
  - [ ] Navigate to Chat section
  - [ ] Ask a question about your uploaded document
  - [ ] Verify you get an answer with source references

- [ ] **Test admin functionality** (if applicable)
  - [ ] Logout and login as: jane.admin / admin123
  - [ ] Navigate to Admin section
  - [ ] Verify you can see all documents and progress

## Development Workflow

- [ ] **Learn daily workflows**
  - [ ] Read [Daily Workflows](./daily-workflows.md)
  - [ ] Practice starting/stopping services
  - [ ] Practice making code changes and seeing hot reload

- [ ] **Learn debugging**
  - [ ] Set up IDE debugging for backend (port 5005)
  - [ ] Learn to use browser dev tools for frontend
  - [ ] Practice reading logs and error messages

- [ ] **Learn testing**
  - [ ] Run backend tests: `cd backend && ./gradlew test`
  - [ ] Run frontend tests: `cd frontend && npm test`
  - [ ] Run integration tests

## Troubleshooting Knowledge

- [ ] **Read troubleshooting guide**
  - [ ] Review [Troubleshooting Guide](./troubleshooting.md)
  - [ ] Practice common troubleshooting steps

- [ ] **Practice problem resolution**
  - [ ] Intentionally stop a service and restart it
  - [ ] Practice checking logs when something fails
  - [ ] Practice resetting data when needed

## Team Integration

- [ ] **Code review process**
  - [ ] Understand branching strategy
  - [ ] Learn code review guidelines
  - [ ] Practice creating pull requests

- [ ] **Communication channels**
  - [ ] Join team chat/Slack channels
  - [ ] Know who to contact for different types of issues
  - [ ] Understand escalation process for technical issues

## Final Verification

- [ ] **Complete end-to-end test**
  - [ ] Start all services from scratch
  - [ ] Upload a document
  - [ ] Process it successfully
  - [ ] Query it via chat
  - [ ] Get proper response with references

- [ ] **Performance check**
  - [ ] Verify reasonable startup times
  - [ ] Check memory usage is acceptable
  - [ ] Confirm hot reload works for both backend and frontend

- [ ] **Knowledge check**
  - [ ] Can explain the architecture
  - [ ] Can troubleshoot common issues
  - [ ] Knows where to find documentation
  - [ ] Knows who to ask for help

## Onboarding Complete! 🎉

You're now ready to contribute to the RAG application development!

### Next Steps
- [ ] Pick up your first development task
- [ ] Set up your preferred IDE configuration
- [ ] Explore the codebase and understand the module structure
- [ ] Join the next team standup/meeting

### Quick Reference
- **Frontend**: http://localhost:3000
- **Backend API**: http://localhost:8081/api
- **API Docs**: http://localhost:8081/q/swagger-ui
- **Keycloak**: http://localhost:8180/admin (admin/admin123)
- **Documentation**: docs/development/
```

## Files / Modules Impacted

- docs/development/README.md
- docs/development/daily-workflows.md
- docs/development/troubleshooting.md
- docs/development/service-management.md
- docs/development/onboarding-checklist.md

## Acceptance Criteria

Given the development documentation is created
When new developers join the project
Then they should be able to follow the onboarding guide successfully

Given daily workflow documentation exists
When developers need to perform common tasks
Then they should have clear step-by-step instructions

Given troubleshooting documentation is available
When issues occur during development
Then developers should be able to resolve them or escalate appropriately

Given service management documentation exists
When developers need to manage individual services
Then they should have clear guidance for all operations

## Testing Requirements

- Test onboarding guide with a new developer
- Verify all documented commands work correctly
- Test troubleshooting steps resolve actual issues
- Validate service management procedures
- Ensure documentation stays up-to-date with changes

## Dependencies / Preconditions

- All development environment components must be implemented
- Scripts and tools referenced in documentation must exist
- Development environment must be fully functional
- Team processes and communication channels must be established