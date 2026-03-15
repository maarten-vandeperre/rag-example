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
    printf 'ERROR: No supported container orchestration tool found.\n'
    exit 1
  fi
}

run_compose() {
  "${COMPOSE_CMD[@]}" -f "${COMPOSE_FILE}" "$@"
}

load_env_file() {
  if [ -f "${ENV_FILE}" ]; then
    set -a
    # shellcheck disable=SC1090
    . "${ENV_FILE}"
    set +a
  fi
}

check_http() {
  local label="$1"
  local url="$2"
  if curl -fsS "${url}" >/dev/null 2>&1; then
    printf '%-12s %s\n' "${label}" 'healthy'
  else
    printf '%-12s %s\n' "${label}" 'unreachable'
  fi
}

check_tcp() {
  local label="$1"
  local host="$2"
  local port="$3"
  if python3 - <<PY >/dev/null 2>&1
import socket
connection = socket.create_connection(("${host}", ${port}), timeout=1)
connection.close()
PY
  then
    printf '%-12s %s\n' "${label}" 'reachable'
  else
    printf '%-12s %s\n' "${label}" 'unreachable'
  fi
}

determine_compose_cmd
load_env_file

printf '=== RAG Application Development Services Status ===\n\n'
printf 'Container Status:\n'
run_compose ps

printf '\n=== Service Health Checks ===\n'
check_tcp 'PostgreSQL' 'localhost' "${DB_PORT:-5432}"
check_http 'Weaviate' "${WEAVIATE_URL:-http://localhost:${WEAVIATE_PORT:-8080}}/v1/meta"
check_http 'Keycloak' "${KEYCLOAK_URL:-http://localhost:${KEYCLOAK_PORT:-8180}}/health/ready"
check_tcp 'Redis' 'localhost' "${REDIS_PORT:-6379}"
check_http 'Ollama' "${OLLAMA_URL:-http://localhost:${OLLAMA_PORT:-11434}}/api/tags"

printf '\n=== Quick Actions ===\n'
printf 'Start services:   ./start-dev-services.sh\n'
printf 'Stop services:    ./stop-dev-services.sh\n'
printf 'View logs:        %s -f %s logs [service-name]\n' "${COMPOSE_CMD[*]}" 'docker-compose.dev.yml'
printf 'Restart service:  %s -f %s restart [service-name]\n' "${COMPOSE_CMD[*]}" 'docker-compose.dev.yml'
