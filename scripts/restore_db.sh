#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
ENV_FILE="${REPO_ROOT}/.env"
BACKUP_DIR="${REPO_ROOT}/backups"
INPUT_PATH="${1:-${BACKUP_DIR}/sanctuary_latest.dump}"

if [[ -f "${ENV_FILE}" ]]; then
  # shellcheck disable=SC1090
  source "${ENV_FILE}"
fi

: "${POSTGRES_DB:?POSTGRES_DB must be set in .env}"
: "${POSTGRES_USER:?POSTGRES_USER must be set in .env}"
: "${POSTGRES_PASSWORD:?POSTGRES_PASSWORD must be set in .env}"

if [[ ! -f "${INPUT_PATH}" ]]; then
  echo "Backup file not found: ${INPUT_PATH}" >&2
  exit 1
fi

export PGPASSWORD="${POSTGRES_PASSWORD}"

echo "Restoring PostgreSQL database from:"
echo "  - ${INPUT_PATH}"

docker exec -e PGPASSWORD="${POSTGRES_PASSWORD}" sanctuary-postgres \
  psql --username="${POSTGRES_USER}" --dbname="${POSTGRES_DB}" -v ON_ERROR_STOP=1 \
  -c "DROP SCHEMA IF EXISTS public CASCADE; CREATE SCHEMA public;"

if [[ "${INPUT_PATH}" == *.dump ]]; then
  cat "${INPUT_PATH}" | docker exec -i -e PGPASSWORD="${POSTGRES_PASSWORD}" sanctuary-postgres \
    pg_restore --no-owner --no-privileges --username="${POSTGRES_USER}" --dbname="${POSTGRES_DB}"
elif [[ "${INPUT_PATH}" == *.sql ]]; then
  cat "${INPUT_PATH}" | docker exec -i -e PGPASSWORD="${POSTGRES_PASSWORD}" sanctuary-postgres \
    psql --username="${POSTGRES_USER}" --dbname="${POSTGRES_DB}" -v ON_ERROR_STOP=1
else
  echo "Unsupported backup format: ${INPUT_PATH}" >&2
  echo "Use a .dump or .sql backup file." >&2
  exit 1
fi

echo "Restore complete."
