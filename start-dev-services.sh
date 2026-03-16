#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_FILE="${SCRIPT_DIR}/docker-compose.dev.yml"
ENV_FILE="${SCRIPT_DIR}/.env.dev"

determine_compose_cmd() {
  if command -v podman-compose >/dev/null 2>&1; then
    COMPOSE_CMD=(podman-compose)
  else
    printf 'ERROR: podman-compose is required for local service management.\n'
    exit 1
  fi
}

run_compose() {
  "${COMPOSE_CMD[@]}" -f "${COMPOSE_FILE}" "$@"
}

ensure_required_paths() {
  mkdir -p "${SCRIPT_DIR}/infrastructure/database" "${SCRIPT_DIR}/infrastructure/keycloak" "${SCRIPT_DIR}/infrastructure/weaviate" "${SCRIPT_DIR}/infrastructure/neo4j"

  local missing=0
  local file
  for file in \
    "${SCRIPT_DIR}/infrastructure/database/init-dev.sql" \
    "${SCRIPT_DIR}/infrastructure/database/migrate-dev.sql" \
    "${SCRIPT_DIR}/infrastructure/database/sample-dev-data.sql" \
    "${SCRIPT_DIR}/infrastructure/ollama/init-ollama.sh" \
    "${SCRIPT_DIR}/infrastructure/keycloak/dev-realm.json" \
    "${SCRIPT_DIR}/infrastructure/weaviate/init-weaviate-dev.sh" \
    "${SCRIPT_DIR}/infrastructure/weaviate/dev-schema.json" \
    "${SCRIPT_DIR}/infrastructure/neo4j/init-neo4j-dev.sh" \
    "${SCRIPT_DIR}/infrastructure/neo4j/dev-constraints.cypher"; do
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

initialize_database() {
  printf 'Applying database schema migrations...\n'
  if run_compose exec -T postgres-dev psql -U "${DB_USER:-rag_dev_user}" -d "${DB_NAME:-rag_app_dev}" < "${SCRIPT_DIR}/infrastructure/database/migrate-dev.sql" >/dev/null; then
    printf 'Database schema migrations applied\n'
  else
    printf 'ERROR: Failed to apply database schema migrations\n'
    show_service_logs postgres-dev
    exit 1
  fi

  printf 'Checking and loading sample data...\n'
  # Check if sample data already exists
  if run_compose exec -T postgres-dev psql -U "${DB_USER:-rag_dev_user}" -d "${DB_NAME:-rag_app_dev}" -c "SELECT COUNT(*) FROM users WHERE user_id = '22222222-2222-2222-2222-222222222222';" 2>/dev/null | grep -q "1"; then
    printf 'Sample data already loaded\n'
  else
    printf 'Loading essential users...\n'
    # Load essential users directly via SQL to avoid schema conflicts
    if run_compose exec -T postgres-dev psql -U "${DB_USER:-rag_dev_user}" -d "${DB_NAME:-rag_app_dev}" -c "
      INSERT INTO users (user_id, username, email, first_name, last_name, role, keycloak_user_id) VALUES 
      ('22222222-2222-2222-2222-222222222222', 'jane.admin', 'jane.admin@example.com', 'Jane', 'Admin', 'ADMIN', 'jane.admin'),
      ('11111111-1111-1111-1111-111111111111', 'john.doe', 'john.doe@example.com', 'John', 'Doe', 'STANDARD', 'john.doe')
      ON CONFLICT (user_id) DO NOTHING;
    " >/dev/null 2>&1; then
      printf 'Essential users loaded successfully\n'
    else
      printf 'WARNING: Failed to load essential users, but continuing...\n'
      printf 'You can manually add them with:\n'
      printf '  podman-compose -f docker-compose.dev.yml exec -T postgres-dev psql -U %s -d %s -c "INSERT INTO users (user_id, username, email, first_name, last_name, role, keycloak_user_id) VALUES ('"'"'22222222-2222-2222-2222-222222222222'"'"', '"'"'jane.admin'"'"', '"'"'jane.admin@example.com'"'"', '"'"'Jane'"'"', '"'"'Admin'"'"', '"'"'ADMIN'"'"', '"'"'jane.admin'"'"')"\n' "${DB_USER:-rag_dev_user}" "${DB_NAME:-rag_app_dev}"
    fi
  fi
}

initialize_weaviate_schema() {
  printf 'Initializing Weaviate schema...\n'
  if [ -f "${SCRIPT_DIR}/infrastructure/weaviate/init-weaviate-dev.sh" ]; then
    chmod +x "${SCRIPT_DIR}/infrastructure/weaviate/init-weaviate-dev.sh"
    if "${SCRIPT_DIR}/infrastructure/weaviate/init-weaviate-dev.sh"; then
      printf 'Weaviate schema ready\n'
    else
      printf 'WARNING: Weaviate schema initialization failed, but continuing...\n'
      printf 'You can manually initialize later with: ./infrastructure/weaviate/init-weaviate-dev.sh\n'
      if [ -f "${SCRIPT_DIR}/infrastructure/weaviate/troubleshoot-weaviate.sh" ]; then
        printf 'For diagnostics run: ./infrastructure/weaviate/troubleshoot-weaviate.sh\n'
      fi
    fi
  else
    printf 'WARNING: Weaviate initialization script not found, skipping schema setup\n'
    printf 'Schema will need to be created manually later\n'
  fi
}

initialize_neo4j_schema() {
  printf 'Initializing Neo4j schema...\n'
  chmod +x "${SCRIPT_DIR}/infrastructure/neo4j/init-neo4j-dev.sh"
  if "${SCRIPT_DIR}/infrastructure/neo4j/init-neo4j-dev.sh"; then
    printf 'Neo4j schema ready\n'
  else
    printf 'WARNING: Neo4j schema initialization failed, but continuing...\n'
    printf 'You can manually initialize later with: ./infrastructure/neo4j/init-neo4j-dev.sh\n'
  fi
}

start_optional_llm() {
  if [ "${START_LLM:-true}" != "true" ]; then
    return 0
  fi

  printf 'Starting Ollama profile...\n'
  run_compose --profile llm up -d ollama-dev
  wait_for_http 'Ollama' "${OLLAMA_URL:-http://localhost:${OLLAMA_PORT:-11434}}/api/tags" 30 5 ollama-dev

  printf 'Ensuring Ollama models are available...\n'
  chmod +x "${SCRIPT_DIR}/infrastructure/ollama/init-ollama.sh"
  OLLAMA_URL="${OLLAMA_URL:-http://localhost:${OLLAMA_PORT:-11434}}" \
  OLLAMA_PULL_MODELS="${OLLAMA_PULL_MODELS:-${LLM_MODEL:-tinyllama}}" \
    "${SCRIPT_DIR}/infrastructure/ollama/init-ollama.sh"
}

determine_compose_cmd
ensure_required_paths
load_env_file

printf '=== Starting RAG Application Development Services ===\n'
printf 'Using container command: %s\n' "${COMPOSE_CMD[*]}"

run_compose down --remove-orphans >/dev/null 2>&1 || true

printf 'Starting PostgreSQL, Redis, and Neo4j...\n'
run_compose up -d postgres-dev redis-dev neo4j-dev
wait_for_postgres
initialize_database
wait_for_http 'Neo4j Browser' "${NEO4J_HTTP_URL:-http://localhost:${NEO4J_HTTP_PORT:-7474}}" 40 3 neo4j-dev
initialize_neo4j_schema

printf 'Starting Weaviate...\n'
run_compose up -d weaviate-dev
wait_for_http 'Weaviate' "${WEAVIATE_URL:-http://localhost:${WEAVIATE_PORT:-8080}}/v1/meta" 60 3 weaviate-dev
# Additional wait for Weaviate to be fully initialized
printf 'Waiting for Weaviate to be fully initialized...\n'
sleep 10
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
printf 'Neo4j:      %s (bolt: %s, %s/%s)\n' "${NEO4J_HTTP_URL:-http://localhost:${NEO4J_HTTP_PORT:-7474}}" "${NEO4J_URI:-bolt://localhost:${NEO4J_BOLT_PORT:-7687}}" "${NEO4J_USER:-neo4j}" "${NEO4J_PASSWORD:-dev-password}"
if [ "${START_LLM:-true}" = "true" ]; then
  printf 'Ollama:     %s\n' "${OLLAMA_URL:-http://localhost:${OLLAMA_PORT:-11434}}"
fi

printf '\n=== Development Configuration ===\n'
printf 'Backend authentication: DISABLED (development mode)\n'
printf 'CORS headers: Configured for frontend development\n'
printf 'Sample users: Admin (jane.admin) and Standard (john.doe) loaded\n'
printf 'File uploads: PDF, Markdown, and plain text supported\n'
printf 'Duplicate content: ALLOWED (development mode)\n'
printf 'CDI beans: All REST controllers properly configured\n'

printf '\nService Status:\n'
run_compose ps

printf '\nYou can now start the backend and frontend separately:\n'
printf '  Backend:  ./gradlew :backend:quarkusDev\n'
printf '  Frontend: ./gradlew :frontend:dev\n'
printf '\nTo stop services: ./stop-dev-services.sh\n'
printf 'To check status: ./status-dev-services.sh\n'
