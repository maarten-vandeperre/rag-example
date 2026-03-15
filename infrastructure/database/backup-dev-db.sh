#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
BACKUP_DIR="${SCRIPT_DIR}/backups"
TIMESTAMP="$(date +"%Y%m%d_%H%M%S")"
BACKUP_FILE="${BACKUP_DIR}/dev_backup_${TIMESTAMP}.sql"
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-rag_app_dev}"
DB_USER="${DB_USER:-rag_dev_user}"
DB_PASSWORD="${DB_PASSWORD:-rag_dev_password}"

if ! command -v pg_dump >/dev/null 2>&1; then
  printf 'ERROR: pg_dump is required.\n'
  exit 1
fi

mkdir -p "$BACKUP_DIR"
PGPASSWORD="$DB_PASSWORD" pg_dump -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" > "$BACKUP_FILE"

printf 'Backup created: %s\n' "$BACKUP_FILE"
