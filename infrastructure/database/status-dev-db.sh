#!/bin/bash
set -euo pipefail

DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-rag_app_dev}"
DB_USER="${DB_USER:-rag_dev_user}"
DB_PASSWORD="${DB_PASSWORD:-rag_dev_password}"

if ! command -v pg_isready >/dev/null 2>&1 || ! command -v psql >/dev/null 2>&1; then
  printf 'ERROR: pg_isready and psql are required.\n'
  exit 1
fi

printf '=== Development Database Status ===\n'
if PGPASSWORD="$DB_PASSWORD" pg_isready -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" >/dev/null 2>&1; then
  printf 'PostgreSQL is running and accessible.\n\n'
  PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c 'SELECT * FROM user_document_summary;'
  printf '\n'
  PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c 'SELECT * FROM document_processing_status;'
else
  printf 'PostgreSQL is not accessible.\n'
  exit 1
fi
