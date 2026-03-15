#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_FILE="${SCRIPT_DIR}/docker-compose.dev.yml"
ENV_FILE="${SCRIPT_DIR}/.env.dev"

determine_compose_cmd() {
  if command -v podman-compose >/dev/null 2>&1; then
    COMPOSE_CMD=(podman-compose)
  elif command -v docker-compose >/dev/null 2>&1; then
    COMPOSE_CMD=(docker-compose)
  elif command -v docker >/dev/null 2>&1 && docker compose version >/dev/null 2>&1; then
    COMPOSE_CMD=(docker compose)
  else
    printf 'ERROR: Neither podman-compose, docker-compose, nor docker compose is available.\n'
    exit 1
  fi
}

run_compose() {
  "${COMPOSE_CMD[@]}" -f "${COMPOSE_FILE}" "$@"
}

ensure_required_paths() {
  mkdir -p "${SCRIPT_DIR}/infrastructure/database" "${SCRIPT_DIR}/infrastructure/keycloak" "${SCRIPT_DIR}/infrastructure/weaviate"

  local missing=0
  local file
  for file in \
    "${SCRIPT_DIR}/infrastructure/database/init-dev.sql" \
    "${SCRIPT_DIR}/infrastructure/database/sample-dev-data.sql" \
    "${SCRIPT_DIR}/infrastructure/keycloak/dev-realm.json" \
    "${SCRIPT_DIR}/infrastructure/weaviate/init-weaviate-dev.sh" \
    "${SCRIPT_DIR}/infrastructure/weaviate/dev-schema.json"; do
    if [ ! -e "${file}" ]; then
      printf 'ERROR: Required file not found: %s\n' "${file#"${SCRIPT_DIR}/"}"
      missing=1
    fi
  done

  if [ "${missing}" -ne 0 ]; then
    exit 1
  fi
}

load_env_file() {
  if [ -f "${ENV_FILE}" ]; then
    set -a
    # shellcheck disable=SC1090
    . "${ENV_FILE}"
    set +a
    printf 'Loaded development environment from %s\n' "${ENV_FILE#"${SCRIPT_DIR}/"}"
  else
    printf 'No .env.dev found, using compose defaults.\n'
  fi
}

show_service_logs() {
  local service="$1"
  printf 'Recent logs for %s:\n' "${service}"
  run_compose logs --tail 30 "${service}" || true
}

wait_for_postgres() {
  local attempts=30
  local try=1
  printf 'Waiting for PostgreSQL'

  while [ "${try}" -le "${attempts}" ]; do
    if run_compose exec -T postgres-dev pg_isready -U "${DB_USER:-rag_dev_user}" -d "${DB_NAME:-rag_app_dev}" >/dev/null 2>&1; then
      printf ' ready\n'
      return 0
    fi

    printf '.'
    sleep 2
    try=$((try + 1))
  done

  printf '\nERROR: PostgreSQL failed to become ready.\n'
  show_service_logs postgres-dev
  exit 1
}

wait_for_http() {
  local label="$1"
  local url="$2"
  local attempts="$3"
  local delay="$4"
  local service="$5"
  local try=1

  printf 'Waiting for %s' "${label}"
  while [ "${try}" -le "${attempts}" ]; do
    if curl -fsS "${url}" >/dev/null 2>&1; then
      printf ' ready\n'
      return 0
    fi

    printf '.'
    sleep "${delay}"
    try=$((try + 1))
  done

  printf '\nERROR: %s failed to become ready at %s\n' "${label}" "${url}"
  show_service_logs "${service}"
  exit 1
}

initialize_weaviate_schema() {
  printf 'Initializing Weaviate schema...\n'
  "${SCRIPT_DIR}/infrastructure/weaviate/init-weaviate-dev.sh" "${SCRIPT_DIR}/infrastructure/weaviate/dev-schema.json" >/dev/null
  printf 'Weaviate schema ready\n'
}

start_optional_llm() {
  if [ "${START_LLM:-false}" != "true" ]; then
    return 0
  fi

  printf 'Starting Ollama profile...\n'
  run_compose --profile llm up -d ollama-dev
  wait_for_http 'Ollama' "${OLLAMA_URL:-http://localhost:${OLLAMA_PORT:-11434}}/api/tags" 30 5 ollama-dev
}

determine_compose_cmd
ensure_required_paths
load_env_file

printf '=== Starting RAG Application Development Services ===\n'
printf 'Using container command: %s\n' "${COMPOSE_CMD[*]}"

run_compose down --remove-orphans >/dev/null 2>&1 || true

printf 'Starting PostgreSQL and Redis...\n'
run_compose up -d postgres-dev redis-dev
wait_for_postgres

printf 'Starting Weaviate...\n'
run_compose up -d weaviate-dev
wait_for_http 'Weaviate' "${WEAVIATE_URL:-http://localhost:${WEAVIATE_PORT:-8080}}/v1/meta" 30 3 weaviate-dev
initialize_weaviate_schema

printf 'Starting Keycloak...\n'
run_compose up -d keycloak-dev
wait_for_http 'Keycloak' "${KEYCLOAK_URL:-http://localhost:${KEYCLOAK_PORT:-8180}}/health/ready" 60 5 keycloak-dev

start_optional_llm

printf '\n=== Development Services Ready ===\n'
printf 'PostgreSQL: localhost:%s (%s/%s)\n' "${DB_PORT:-5432}" "${DB_USER:-rag_dev_user}" "${DB_PASSWORD:-rag_dev_password}"
printf 'Weaviate:   %s\n' "${WEAVIATE_URL:-http://localhost:${WEAVIATE_PORT:-8080}}"
printf 'Keycloak:   %s (%s/%s)\n' "${KEYCLOAK_URL:-http://localhost:${KEYCLOAK_PORT:-8180}}" "${KEYCLOAK_ADMIN:-admin}" "${KEYCLOAK_ADMIN_PASSWORD:-admin123}"
printf 'Redis:      redis://localhost:%s\n' "${REDIS_PORT:-6379}"
if [ "${START_LLM:-false}" = "true" ]; then
  printf 'Ollama:     %s\n' "${OLLAMA_URL:-http://localhost:${OLLAMA_PORT:-11434}}"
fi

printf '\nService Status:\n'
run_compose ps

printf '\nYou can now start the backend and frontend separately:\n'
printf '  Backend:  ./gradlew :backend:quarkusDev\n'
printf '  Frontend: ./gradlew :frontend:dev\n'
printf '\nTo stop services: ./stop-dev-services.sh\n'
printf 'To check status: ./status-dev-services.sh\n'
