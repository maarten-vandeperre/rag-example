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

port_status() {
  local port="$1"
  local listeners
  listeners="$(lsof -nP -iTCP:"${port}" -sTCP:LISTEN 2>/dev/null || true)"
  if [ -n "${listeners}" ]; then
    printf 'Port %s: in use\n' "${port}"
    printf '%s\n' "${listeners}"
  else
    printf 'Port %s: available\n' "${port}"
  fi
}

check_http() {
  local label="$1"
  local url="$2"
  if curl -fsS --max-time 5 "${url}" >/dev/null 2>&1; then
    printf '%s: reachable\n' "${label}"
  else
    printf '%s: unreachable (%s)\n' "${label}" "${url}"
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
    printf '%s: reachable\n' "${label}"
  else
    printf '%s: unreachable (%s:%s)\n' "${label}" "${host}" "${port}"
  fi
}

print_system_resources() {
  printf '=== System Resources ===\n'
  if [ "$(uname -s)" = 'Darwin' ]; then
    vm_stat
    printf '\nDisk Space:\n'
    df -h .
  else
    free -h 2>/dev/null || printf 'Memory info not available\n'
    printf '\nDisk Space:\n'
    df -h . 2>/dev/null || printf 'Disk info not available\n'
  fi
}

determine_compose_cmd
load_env_file

printf '=== RAG Development Services Troubleshooting ===\n'
printf 'Container command: %s\n\n' "${COMPOSE_CMD[*]}"

print_system_resources

printf '\n=== Port Availability ===\n'
port_status "${DB_PORT:-5432}"
port_status "${WEAVIATE_PORT:-8080}"
port_status "${KEYCLOAK_PORT:-8180}"
port_status "${REDIS_PORT:-6379}"
port_status "${NEO4J_HTTP_PORT:-7474}"
port_status "${NEO4J_BOLT_PORT:-7687}"
port_status "${OLLAMA_PORT:-11434}"

printf '\n=== Container Status ===\n'
run_compose ps || true

printf '\n=== Recent Container Logs ===\n'
for service in postgres-dev weaviate-dev keycloak-dev redis-dev neo4j-dev ollama-dev; do
  printf '\n--- %s ---\n' "${service}"
  run_compose logs --tail 10 "${service}" 2>/dev/null || printf 'No logs available\n'
done

printf '\n=== Connectivity Checks ===\n'
check_tcp 'PostgreSQL' 'localhost' "${DB_PORT:-5432}"
check_http 'Weaviate' "${WEAVIATE_URL:-http://localhost:${WEAVIATE_PORT:-8080}}/v1/meta"
check_http 'Keycloak' "${KEYCLOAK_URL:-http://localhost:${KEYCLOAK_PORT:-8180}}/health/ready"
check_tcp 'Redis' 'localhost' "${REDIS_PORT:-6379}"
check_http 'Neo4j HTTP' "${NEO4J_HTTP_URL:-http://localhost:${NEO4J_HTTP_PORT:-7474}}"
check_tcp 'Neo4j Bolt' 'localhost' "${NEO4J_BOLT_PORT:-7687}"
check_http 'Ollama' "${OLLAMA_URL:-http://localhost:${OLLAMA_PORT:-11434}}/api/tags"

printf '\n=== Recommended Actions ===\n'
printf '1. Stop any conflicting process shown in the port list.\n'
printf '2. Inspect service logs with %s -f docker-compose.dev.yml logs [service-name].\n' "${COMPOSE_CMD[*]}"
printf '3. Restart a failing service with %s -f docker-compose.dev.yml restart [service-name].\n' "${COMPOSE_CMD[*]}"
printf '4. Retry the stack with ./stop-dev-services.sh && ./start-dev-services.sh.\n'
printf '5. If persistent state is corrupted, use ./stop-dev-services.sh --clean before restarting.\n'
