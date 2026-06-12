#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
ENV_FILE="${REPO_ROOT}/.env"
ACTION="${1:-}"
EMAIL="${2:-}"

if [[ -f "${ENV_FILE}" ]]; then
  # shellcheck disable=SC1090
  source "${ENV_FILE}"
fi

POSTGRES_DB="${POSTGRES_DB:-sanctuary}"
POSTGRES_USER="${POSTGRES_USER:-${SANCTUARY_DB_USERNAME:-sanctuary}}"
POSTGRES_PASSWORD="${POSTGRES_PASSWORD:-${SANCTUARY_DB_PASSWORD:-change-me-now}}"

usage() {
  echo "Usage:"
  echo "  $0 grant user@example.com"
  echo "  $0 revoke user@example.com"
}

if [[ "${ACTION}" != "grant" && "${ACTION}" != "revoke" ]] || [[ -z "${EMAIL}" ]]; then
  usage >&2
  exit 1
fi

if [[ "${ACTION}" == "grant" ]]; then
  SQL=$(cat <<'SQL'
    WITH target_user AS (
      SELECT id
      FROM users
      WHERE lower(email) = lower(:'email')
    )
    INSERT INTO ask_sanctuary_user_entitlements (
      user_id,
      tier,
      unlimited,
      updated_at
    )
    SELECT id, 'ADMIN', true, NOW()
    FROM target_user
    ON CONFLICT (user_id)
    DO UPDATE SET
      tier = 'ADMIN',
      unlimited = true,
      daily_limit_override = NULL,
      updated_at = NOW()
    RETURNING user_id, tier, unlimited;
SQL
  )
else
  SQL=$(cat <<'SQL'
    DELETE FROM ask_sanctuary_user_entitlements
    WHERE user_id IN (
      SELECT id
      FROM users
      WHERE lower(email) = lower(:'email')
    )
    RETURNING user_id, tier, unlimited;
SQL
  )
fi

echo "Applying Ask Sanctuary admin ${ACTION} for ${EMAIL}"

printf "%s\n" "${SQL}" | docker exec -i -e PGPASSWORD="${POSTGRES_PASSWORD}" sanctuary-postgres \
  psql --username="${POSTGRES_USER}" --dbname="${POSTGRES_DB}" -v ON_ERROR_STOP=1 \
  -v email="${EMAIL}"
