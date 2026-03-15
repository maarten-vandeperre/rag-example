# Developer Onboarding Checklist

## Prerequisites

- [ ] Install Podman and `podman-compose`
- [ ] Install Java 25
- [ ] Install Node.js 18+
- [ ] Install Git
- [ ] Install `curl`, `jq`, and PostgreSQL client tools

## Repository Setup

- [ ] Clone the repo
- [ ] Run `chmod +x *.sh backend/*.sh frontend/*.sh infrastructure/**/*.sh`
- [ ] Run `./setup.sh`

## Environment Setup

- [ ] Run `./start-dev-services.sh`
- [ ] Run `./status-dev-services.sh`
- [ ] Run `./infrastructure/weaviate/init-weaviate-dev.sh`
- [ ] Run `./infrastructure/weaviate/load-sample-data.sh`

## App Setup

- [ ] Start backend with `cd backend && ./start-dev.sh`
- [ ] Run `cd backend && ./test-dev-integration.sh`
- [ ] Start frontend with `cd frontend && ./start-dev.sh`
- [ ] Run `cd frontend && ./test-dev-integration.sh`

## First Manual Test

- [ ] Open `http://localhost:3000`
- [ ] Login with `john.doe / password123`
- [ ] Upload a document
- [ ] Ask a chat question
- [ ] Verify references are shown

## Learn the Workflow

- [ ] Read `docs/development/daily-workflows.md`
- [ ] Read `docs/development/troubleshooting.md`
- [ ] Read `docs/development/service-management.md`

## Final Check

- [ ] Can explain the local architecture
- [ ] Can restart services and apps
- [ ] Can reset database and vector data
- [ ] Knows where to find help
