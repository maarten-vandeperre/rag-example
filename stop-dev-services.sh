#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_FILE="${SCRIPT_DIR}/docker-compose.dev.yml"

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

determine_compose_cmd

printf '=== Stopping RAG Application Development Services ===\n'
printf 'Stopping containers...\n'
run_compose down --remove-orphans

if [ "${1:-}" = "--clean" ] || [ "${1:-}" = "-c" ]; then
  printf 'Removing volumes...\n'
  run_compose down --volumes
  printf 'All development services and volumes removed.\n'
else
  printf 'Development services stopped. Data volumes were preserved.\n'
  printf 'Use %s --clean to remove the stored data as well.\n' "$0"
fi
