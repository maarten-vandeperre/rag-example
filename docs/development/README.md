# RAG Application Development Environment

This guide covers the local development workflow for the RAG application.

## Overview

The local environment uses a hybrid setup:
- Supporting services run in Podman containers.
- Backend and frontend run natively for faster iteration.

## Architecture

```text
Frontend (React dev, 3000) <-> Backend (Quarkus dev, 8081) <-> PostgreSQL (5432)
                                                          <-> Weaviate (8080)
                                                          <-> Keycloak (8180)
                                                          <-> Redis (6379)
                                                          <-> Ollama (11434)
```

## Quick Start

### Prerequisites

- Podman and `podman-compose`
- Java 25
- Node.js 18+
- Git

### Setup

```bash
git clone <repository-url>
cd rag-example
chmod +x *.sh backend/*.sh frontend/*.sh infrastructure/**/*.sh
./setup.sh
```

### Start Services

```bash
./start-dev-services.sh
./status-dev-services.sh
```

### Start Applications

```bash
cd backend && ./start-dev.sh
cd frontend && ./start-dev.sh
```

## Access

- Frontend: `http://localhost:3000`
- Backend API: `http://localhost:8081/api`
- Swagger UI: `http://localhost:8081/q/swagger-ui`
- Keycloak Admin: `http://localhost:8180/admin`

## Development Credentials

- `john.doe / password123`
- `jane.admin / admin123`
- `test.user / test123`

## More Guides

- `docs/development/daily-workflows.md`
- `docs/development/troubleshooting.md`
- `docs/development/service-management.md`
- `docs/development/onboarding-checklist.md`
