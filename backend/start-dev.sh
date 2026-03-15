#!/bin/bash
set -euo pipefail

printf '=== Starting RAG Backend in Development Mode ===\n'

check_http() {
  local label="$1"
  local url="$2"
  if ! curl -fsS "$url" >/dev/null 2>&1; then
    printf 'ERROR: %s is not reachable at %s\n' "$label" "$url"
    printf 'Please start development services first: ./start-dev-services.sh\n'
    exit 1
  fi
  printf '%s is running\n' "$label"
}

if ! pg_isready -h localhost -p 5432 -U rag_dev_user >/dev/null 2>&1; then
  printf 'ERROR: PostgreSQL is not running\n'
  printf 'Please start development services first: ./start-dev-services.sh\n'
  exit 1
fi
printf 'PostgreSQL is running\n'

check_http 'Weaviate' 'http://localhost:8080/v1/meta'
check_http 'Keycloak' 'http://localhost:8180/health/ready'

mkdir -p ./storage/documents
printf 'Storage directory ready\n\n'
printf 'Backend: http://localhost:8081\n'
printf 'Swagger UI: http://localhost:8081/q/swagger-ui\n'
printf 'Health Check: http://localhost:8081/q/health\n\n'

QUARKUS_PROFILE=dev JAVA_OPTS="${JAVA_OPTS:-} -Xmx2g -XX:+UseG1GC -Dquarkus.profile=dev" ./gradlew quarkusDev
