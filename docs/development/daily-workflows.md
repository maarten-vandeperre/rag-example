# Daily Development Workflows

## Start of Day

```bash
./status-dev-services.sh
./start-dev-services.sh
cd backend && ./start-dev.sh
cd frontend && ./start-dev.sh
```

## Verify Environment

```bash
cd backend && ./test-dev-integration.sh
cd frontend && ./test-dev-integration.sh
```

## Common Tasks

### Database

```bash
./infrastructure/database/status-dev-db.sh
./infrastructure/database/reset-dev-db.sh
./infrastructure/database/backup-dev-db.sh
```

### Weaviate

```bash
./infrastructure/weaviate/manage-weaviate-dev.sh status
./infrastructure/weaviate/manage-weaviate-dev.sh load-sample
./infrastructure/weaviate/manage-weaviate-dev.sh test
```

### Keycloak

```bash
./infrastructure/keycloak/test-auth.sh
./infrastructure/keycloak/configure-dev-realm.sh
```

## Hot Reload

- Backend Java changes reload in Quarkus dev mode.
- Frontend React changes reload in the dev server.
- Restart apps after changing env files.

## End of Day

```bash
./stop-dev-services.sh
```
