#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
ENV_FILE="${REPO_ROOT}/.env"
BACKUP_DIR="${REPO_ROOT}/backups"
TIMESTAMP="$(date +%Y%m%d-%H%M%S)"

if [[ -f "${ENV_FILE}" ]]; then
  # shellcheck disable=SC1090
  source "${ENV_FILE}"
fi

: "${POSTGRES_DB:?POSTGRES_DB must be set in .env}"
: "${POSTGRES_USER:?POSTGRES_USER must be set in .env}"
: "${POSTGRES_PASSWORD:?POSTGRES_PASSWORD must be set in .env}"

mkdir -p "${BACKUP_DIR}"

export PGPASSWORD="${POSTGRES_PASSWORD}"

CUSTOM_DUMP_PATH="${BACKUP_DIR}/sanctuary-${TIMESTAMP}.dump"
SQL_DUMP_PATH="${BACKUP_DIR}/sanctuary-${TIMESTAMP}.sql"
LATEST_CUSTOM_DUMP_PATH="${BACKUP_DIR}/sanctuary_latest.dump"
LATEST_SQL_DUMP_PATH="${BACKUP_DIR}/sanctuary_latest.sql"

echo "Exporting PostgreSQL database to:"
echo "  - ${CUSTOM_DUMP_PATH}"
echo "  - ${SQL_DUMP_PATH}"

docker exec -e PGPASSWORD="${POSTGRES_PASSWORD}" sanctuary-postgres \
  pg_dump --format=custom --no-owner --no-privileges --username="${POSTGRES_USER}" --dbname="${POSTGRES_DB}" \
  > "${CUSTOM_DUMP_PATH}"

docker exec -e PGPASSWORD="${POSTGRES_PASSWORD}" sanctuary-postgres \
  pg_dump --format=plain --no-owner --no-privileges --username="${POSTGRES_USER}" --dbname="${POSTGRES_DB}" \
  > "${SQL_DUMP_PATH}"

cp "${CUSTOM_DUMP_PATH}" "${LATEST_CUSTOM_DUMP_PATH}"
cp "${SQL_DUMP_PATH}" "${LATEST_SQL_DUMP_PATH}"

echo "Export complete."
echo "Latest copies:"
echo "  - ${LATEST_CUSTOM_DUMP_PATH}"
echo "  - ${LATEST_SQL_DUMP_PATH}"
