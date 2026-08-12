#!/usr/bin/env bash

set -euo pipefail

readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly CONTROL_SCRIPT="${SCRIPT_DIR}/dev-environment-control.sh"

TEST_ROOT="$(mktemp -d)"
trap 'rm -rf "${TEST_ROOT}"' EXIT

pass_count=0

fail() {
  echo "TEST FAILURE: $*" >&2
  exit 1
}

assert_log_contains() {
  local pattern="$1"
  grep -Fq -- "${pattern}" "${MOCK_STATE_DIR}/aws.log" || fail "AWS log did not contain '${pattern}'."
}

assert_log_not_contains() {
  local pattern="$1"
  if grep -Fq -- "${pattern}" "${MOCK_STATE_DIR}/aws.log"; then
    fail "AWS log unexpectedly contained '${pattern}'."
  fi
}

new_scenario() {
  local name="$1"
  MOCK_STATE_DIR="${TEST_ROOT}/${name}"
  export MOCK_STATE_DIR
  mkdir -p "${MOCK_STATE_DIR}/bin"
  : > "${MOCK_STATE_DIR}/aws.log"
  : > "${MOCK_STATE_DIR}/curl_states"

  cp "${TEST_ROOT}/mock-aws" "${MOCK_STATE_DIR}/bin/aws"
  cp "${TEST_ROOT}/mock-curl" "${MOCK_STATE_DIR}/bin/curl"
  chmod +x "${MOCK_STATE_DIR}/bin/aws" "${MOCK_STATE_DIR}/bin/curl"

  export AWS_COMMAND="${MOCK_STATE_DIR}/bin/aws"
  export CURL_COMMAND="${MOCK_STATE_DIR}/bin/curl"
  export WAIT_INTERVAL_SECONDS=0
  export RDS_WAIT_MAX_ATTEMPTS=12
  export ECS_WAIT_MAX_ATTEMPTS=12
  export HEALTH_WAIT_MAX_ATTEMPTS=12
  export AWS_REGION=us-east-1
  export EXPECTED_AWS_ACCOUNT_ID=160885294528
  export ECS_CLUSTER=sanctuary-dev
  export ECS_SERVICE=sanctuary-api-dev
  export RDS_INSTANCE=sanctuary-dev-db
  export DEV_HEALTH_URL=https://dev-api.mydailysanctuary.com/health
}

run_control() {
  bash "${CONTROL_SCRIPT}" "$1" > "${MOCK_STATE_DIR}/output.log" 2>&1
}

record_pass() {
  pass_count=$((pass_count + 1))
  echo "PASS: $1"
}

cat > "${TEST_ROOT}/mock-aws" <<'MOCK_AWS'
#!/usr/bin/env bash
set -euo pipefail

echo "$*" >> "${MOCK_STATE_DIR}/aws.log"

consume_state() {
  local file="$1"
  local fallback_file="${file}.last"
  local value

  if [[ -s "${file}" ]]; then
    value="$(head -n 1 "${file}")"
    tail -n +2 "${file}" > "${file}.next"
    mv "${file}.next" "${file}"
    printf '%s' "${value}" > "${fallback_file}"
    printf '%s\n' "${value}"
    return
  fi

  [[ -f "${fallback_file}" ]] || exit 90
  cat "${fallback_file}"
  printf '\n'
}

case "${1:-} ${2:-}" in
  "sts get-caller-identity")
    printf '%s\n' "${MOCK_ACCOUNT_ID:-160885294528}"
    ;;
  "ecs describe-services")
    if [[ "$*" == *"services[0].status"* ]]; then
      printf '%s\n' "${MOCK_ECS_SERVICE_STATUS:-ACTIVE}"
      exit 0
    fi

    ecs_value="$(consume_state "${MOCK_STATE_DIR}/ecs_states")"
    if [[ "$*" == *"status,desiredCount,runningCount,pendingCount"* ]]; then
      printf 'ACTIVE\t%s\n' "${ecs_value}"
    else
      printf '%s\n' "${ecs_value}"
    fi
    ;;
  "ecs update-service")
    ;;
  "rds describe-db-instances")
    consume_state "${MOCK_STATE_DIR}/rds_states"
    ;;
  "rds stop-db-instance"|"rds start-db-instance")
    ;;
  *)
    echo "Unsupported mock AWS call: $*" >&2
    exit 91
    ;;
esac
MOCK_AWS

cat > "${TEST_ROOT}/mock-curl" <<'MOCK_CURL'
#!/usr/bin/env bash
set -euo pipefail

state_file="${MOCK_STATE_DIR}/curl_states"
if [[ ! -s "${state_file}" ]]; then
  exit 0
fi

result="$(head -n 1 "${state_file}")"
tail -n +2 "${state_file}" > "${state_file}.next"
mv "${state_file}.next" "${state_file}"
[[ "${result}" == "success" ]]
MOCK_CURL

chmod +x "${TEST_ROOT}/mock-aws" "${TEST_ROOT}/mock-curl"

new_scenario status
printf '%s\n' '1 1 0' '1 1 0' > "${MOCK_STATE_DIR}/ecs_states"
printf '%s\n' available available > "${MOCK_STATE_DIR}/rds_states"
run_control status
assert_log_not_contains "update-service"
assert_log_not_contains "stop-db-instance"
assert_log_not_contains "start-db-instance"
record_pass "status is read-only"

new_scenario stop_available
printf '%s\n' '1 1 0' '0 1 0' '0 0 0' '0 0 0' > "${MOCK_STATE_DIR}/ecs_states"
printf '%s\n' available available stopping stopped stopped > "${MOCK_STATE_DIR}/rds_states"
run_control stop
assert_log_contains "ecs update-service"
assert_log_contains "--desired-count 0"
assert_log_contains "rds stop-db-instance"
assert_log_not_contains "rds wait"
[[ "$(grep -n 'ecs update-service' "${MOCK_STATE_DIR}/aws.log" | cut -d: -f1)" -lt "$(grep -n 'rds stop-db-instance' "${MOCK_STATE_DIR}/aws.log" | cut -d: -f1)" ]] || fail "RDS stop occurred before ECS scale-down."
record_pass "stop scales ECS to zero and polls RDS to stopped"

new_scenario stop_idempotent
printf '%s\n' '0 0 0' '0 0 0' '0 0 0' > "${MOCK_STATE_DIR}/ecs_states"
printf '%s\n' stopped stopped stopped > "${MOCK_STATE_DIR}/rds_states"
run_control stop
assert_log_not_contains "rds stop-db-instance"
record_pass "stop is idempotent when RDS is already stopped"

new_scenario stop_already_stopping
printf '%s\n' '0 0 0' '0 0 0' '0 0 0' > "${MOCK_STATE_DIR}/ecs_states"
printf '%s\n' stopping stopping stopping stopped stopped > "${MOCK_STATE_DIR}/rds_states"
run_control stop
assert_log_not_contains "rds stop-db-instance"
record_pass "stop waits for an existing RDS shutdown without issuing a duplicate request"

new_scenario stop_during_automatic_restart
printf '%s\n' '0 0 0' '0 0 0' '0 0 0' > "${MOCK_STATE_DIR}/ecs_states"
printf '%s\n' starting starting available stopping stopped stopped > "${MOCK_STATE_DIR}/rds_states"
run_control stop
assert_log_contains "rds stop-db-instance"
record_pass "nightly stop handles the automatic RDS restart state"

new_scenario start_stopped
printf '%s\n' '0 0 0' '1 0 1' '1 1 0' '1 1 0' > "${MOCK_STATE_DIR}/ecs_states"
printf '%s\n' stopped stopped starting available available > "${MOCK_STATE_DIR}/rds_states"
printf '%s\n' failure success > "${MOCK_STATE_DIR}/curl_states"
run_control start
assert_log_contains "rds start-db-instance"
assert_log_contains "--desired-count 1"
[[ "$(grep -n 'rds start-db-instance' "${MOCK_STATE_DIR}/aws.log" | cut -d: -f1)" -lt "$(grep -n 'ecs update-service' "${MOCK_STATE_DIR}/aws.log" | cut -d: -f1)" ]] || fail "ECS start occurred before the RDS start."
record_pass "start waits for RDS, starts ECS, and verifies health"

new_scenario start_while_stopping
printf '%s\n' '0 0 0' '1 1 0' '1 1 0' > "${MOCK_STATE_DIR}/ecs_states"
printf '%s\n' stopping stopping stopped starting available available > "${MOCK_STATE_DIR}/rds_states"
run_control start
assert_log_contains "rds start-db-instance"
assert_log_contains "--desired-count 1"
record_pass "start waits for an in-progress stop before restarting RDS"

new_scenario start_during_maintenance
printf '%s\n' '0 0 0' '1 1 0' '1 1 0' > "${MOCK_STATE_DIR}/ecs_states"
printf '%s\n' modifying modifying available available > "${MOCK_STATE_DIR}/rds_states"
run_control start
assert_log_not_contains "rds start-db-instance"
assert_log_contains "--desired-count 1"
record_pass "start waits out non-failure RDS maintenance without issuing an invalid start"

new_scenario reject_production
printf '%s\n' '1 1 0' > "${MOCK_STATE_DIR}/ecs_states"
printf '%s\n' available > "${MOCK_STATE_DIR}/rds_states"
export ECS_SERVICE=sanctuary-api-prod
if run_control stop; then
  fail "Production ECS target was not rejected."
fi
assert_log_not_contains "update-service"
assert_log_not_contains "stop-db-instance"
record_pass "production target is rejected before AWS mutation"

new_scenario reject_missing_ecs
printf '%s\n' '1 1 0' > "${MOCK_STATE_DIR}/ecs_states"
printf '%s\n' available > "${MOCK_STATE_DIR}/rds_states"
export MOCK_ECS_SERVICE_STATUS=INACTIVE
if run_control stop; then
  fail "Inactive ECS service was not rejected."
fi
unset MOCK_ECS_SERVICE_STATUS
assert_log_not_contains "update-service"
assert_log_not_contains "stop-db-instance"
record_pass "inactive or missing ECS service is rejected before mutation"

new_scenario rds_failure
printf '%s\n' '0 0 0' '0 0 0' > "${MOCK_STATE_DIR}/ecs_states"
printf '%s\n' storage-full storage-full > "${MOCK_STATE_DIR}/rds_states"
if run_control stop; then
  fail "RDS failure state did not fail the action."
fi
assert_log_not_contains "rds stop-db-instance"
record_pass "RDS failure states stop automation safely"

new_scenario rds_timeout
printf '%s\n' '0 0 0' '0 0 0' > "${MOCK_STATE_DIR}/ecs_states"
printf '%s\n' stopping stopping > "${MOCK_STATE_DIR}/rds_states"
export RDS_WAIT_MAX_ATTEMPTS=2
if run_control stop; then
  fail "RDS timeout did not fail the action."
fi
assert_log_not_contains "rds stop-db-instance"
record_pass "RDS polling fails clearly after its bounded timeout"

echo "${pass_count} DEV environment control tests passed."
