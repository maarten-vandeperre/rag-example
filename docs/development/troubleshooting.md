# Troubleshooting Guide

## Services Won't Start

### Port already in use

```bash
lsof -i :5432
lsof -i :8080
lsof -i :8180
```

### Podman resource issues

```bash
podman system df
podman system prune
```

### Runtime command detection problems

The lifecycle scripts support `podman-compose`, `docker-compose`, and `docker compose`.

If service management behaves unexpectedly, first confirm which command is available on your machine:

```bash
podman-compose version
docker-compose version
docker compose version
```

## Backend Issues

### Database connection refused

```bash
podman ps | grep postgres
podman logs rag-postgres-dev
pg_isready -h localhost -p 5432 -U rag_dev_user
```

### Keycloak auth failures

```bash
curl http://localhost:8180/health/ready
./infrastructure/keycloak/test-auth.sh
./infrastructure/keycloak/configure-dev-realm.sh
```

Keycloak now uses PostgreSQL in `docker-compose.dev.yml`, so a Keycloak startup failure can also be caused by a PostgreSQL readiness problem.

## Frontend Issues

### CORS errors

- Confirm backend is on `http://localhost:8081`.
- Check `backend/src/main/resources/application-dev.properties`.
- Retry after clearing browser state.

## Weaviate Issues

### Schema missing

```bash
./infrastructure/weaviate/init-weaviate-dev.sh
./infrastructure/weaviate/manage-weaviate-dev.sh schema
```

`./start-dev-services.sh` also attempts schema initialization automatically after Weaviate becomes reachable.

### No vector results

```bash
./infrastructure/weaviate/manage-weaviate-dev.sh load-sample
./infrastructure/weaviate/manage-weaviate-dev.sh test
```

Remember that the sample vectors are smoke-test data and do not match the backend's configured 384-dimensional embedding setting.

## Service Script Diagnostics

```bash
./status-dev-services.sh
./troubleshoot-dev-services.sh
```

Use `./troubleshoot-dev-services.sh` when you need:

- current port usage
- recent container logs
- direct connectivity checks
- a suggested recovery path

## Full Reset

```bash
./stop-dev-services.sh --clean
./start-dev-services.sh
```
