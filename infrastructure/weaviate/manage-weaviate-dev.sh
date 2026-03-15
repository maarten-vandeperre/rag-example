#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
WEAVIATE_URL="${WEAVIATE_URL:-http://localhost:8080}"

if ! command -v curl >/dev/null 2>&1 || ! command -v jq >/dev/null 2>&1; then
  printf 'ERROR: curl and jq are required.\n'
  exit 1
fi

show_help() {
  printf 'Usage: %s [status|schema|reset|backup|load-sample|test|help]\n' "$0"
}

show_status() {
  curl -fsS "${WEAVIATE_URL}/v1/meta" | jq '{hostname, version}'
  printf 'DocumentChunk objects: '
  curl -fsS "${WEAVIATE_URL}/v1/objects?class=DocumentChunk" | jq '.objects | length'
  printf 'UserQuery objects: '
  curl -fsS "${WEAVIATE_URL}/v1/objects?class=UserQuery" | jq '.objects | length'
}

show_schema() {
  curl -fsS "${WEAVIATE_URL}/v1/schema" | jq '.classes[] | {class: .class, properties: [.properties[] | {name: .name, dataType: .dataType}]}'
}

reset_data() {
  curl -fsS -X DELETE "${WEAVIATE_URL}/v1/schema/UserQuery" >/dev/null || true
  curl -fsS -X DELETE "${WEAVIATE_URL}/v1/schema/DocumentChunk" >/dev/null || true
  "${SCRIPT_DIR}/init-weaviate-dev.sh"
}

backup_data() {
  mkdir -p "${SCRIPT_DIR}/backups"
  local file="${SCRIPT_DIR}/backups/weaviate_backup_$(date +%Y%m%d_%H%M%S).json"
  curl -fsS "${WEAVIATE_URL}/v1/objects" > "$file"
  printf 'Backup created: %s\n' "$file"
}

case "${1:-help}" in
  status) show_status ;;
  schema) show_schema ;;
  reset) reset_data ;;
  backup) backup_data ;;
  load-sample) "${SCRIPT_DIR}/load-sample-data.sh" ;;
  test) "${SCRIPT_DIR}/test-vector-search.sh" ;;
  help|*) show_help ;;
esac
