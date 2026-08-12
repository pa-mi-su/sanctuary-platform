#!/usr/bin/env bash

set -euo pipefail

readonly EXPECTED_ACCOUNT="160885294528"
readonly EXPECTED_REGION="us-east-1"
readonly EXPECTED_ECS_CLUSTER="sanctuary-dev"
readonly EXPECTED_ECS_SERVICE="sanctuary-api-dev"
readonly EXPECTED_RDS_INSTANCE="sanctuary-dev-db"

readonly AWS_COMMAND="${AWS_COMMAND:-aws}"
readonly CURL_COMMAND="${CURL_COMMAND:-curl}"
readonly WAIT_INTERVAL_SECONDS="${WAIT_INTERVAL_SECONDS:-15}"
readonly RDS_WAIT_MAX_ATTEMPTS="${RDS_WAIT_MAX_ATTEMPTS:-320}"
readonly ECS_WAIT_MAX_ATTEMPTS="${ECS_WAIT_MAX_ATTEMPTS:-80}"
readonly HEALTH_WAIT_MAX_ATTEMPTS="${HEALTH_WAIT_MAX_ATTEMPTS:-60}"

readonly ACTION="${1:-}"
readonly AWS_REGION="${AWS_REGION:-}"
readonly EXPECTED_AWS_ACCOUNT_ID="${EXPECTED_AWS_ACCOUNT_ID:-}"
readonly ECS_CLUSTER="${ECS_CLUSTER:-}"
readonly ECS_SERVICE="${ECS_SERVICE:-}"
readonly RDS_INSTANCE="${RDS_INSTANCE:-}"
readonly DEV_HEALTH_URL="${DEV_HEALTH_URL:-}"

die() {
  echo "ERROR: $*" >&2
  exit 1
}

require_positive_integer() {
  local name="$1"
  local value="$2"
  [[ "${value}" =~ ^[1-9][0-9]*$ ]] || die "${name} must be a positive integer, got '${value}'."
}

sleep_between_checks() {
  if [[ "${WAIT_INTERVAL_SECONDS}" != "0" ]]; then
    sleep "${WAIT_INTERVAL_SECONDS}"
  fi
}

aws_cli() {
  "${AWS_COMMAND}" "$@"
}

validate_targets() {
  case "${ACTION}" in
    status|start|stop) ;;
    *) die "Unsupported action '${ACTION}'. Expected status, start, or stop." ;;
  esac

  [[ "${AWS_REGION}" == "${EXPECTED_REGION}" ]] || die "Refusing unexpected AWS region '${AWS_REGION}'."
  [[ "${EXPECTED_AWS_ACCOUNT_ID}" == "${EXPECTED_ACCOUNT}" ]] || die "Refusing unexpected configured AWS account '${EXPECTED_AWS_ACCOUNT_ID}'."
  [[ "${ECS_CLUSTER}" == "${EXPECTED_ECS_CLUSTER}" ]] || die "Refusing unexpected ECS cluster '${ECS_CLUSTER}'."
  [[ "${ECS_SERVICE}" == "${EXPECTED_ECS_SERVICE}" ]] || die "Refusing unexpected ECS service '${ECS_SERVICE}'."
  [[ "${RDS_INSTANCE}" == "${EXPECTED_RDS_INSTANCE}" ]] || die "Refusing unexpected RDS instance '${RDS_INSTANCE}'."
  [[ -n "${DEV_HEALTH_URL}" ]] || die "DEV_HEALTH_URL is required."

  require_positive_integer RDS_WAIT_MAX_ATTEMPTS "${RDS_WAIT_MAX_ATTEMPTS}"
  require_positive_integer ECS_WAIT_MAX_ATTEMPTS "${ECS_WAIT_MAX_ATTEMPTS}"
  require_positive_integer HEALTH_WAIT_MAX_ATTEMPTS "${HEALTH_WAIT_MAX_ATTEMPTS}"
  [[ "${WAIT_INTERVAL_SECONDS}" =~ ^[0-9]+$ ]] || die "WAIT_INTERVAL_SECONDS must be a non-negative integer."

  local actual_account_id
  actual_account_id="$(aws_cli sts get-caller-identity --query Account --output text)"
  [[ "${actual_account_id}" == "${EXPECTED_ACCOUNT}" ]] || die "Refusing AWS account '${actual_account_id}'."

  local ecs_service_status
  ecs_service_status="$(aws_cli ecs describe-services \
    --cluster "${ECS_CLUSTER}" \
    --services "${ECS_SERVICE}" \
    --region "${AWS_REGION}" \
    --query 'services[0].status' \
    --output text)"
  [[ "${ecs_service_status}" == "ACTIVE" ]] || die "DEV ECS service is not active; reported status '${ecs_service_status}'."

  echo "Validated DEV-only action '${ACTION}' in ${actual_account_id}/${AWS_REGION}."
}

ecs_summary() {
  aws_cli ecs describe-services \
    --cluster "${ECS_CLUSTER}" \
    --services "${ECS_SERVICE}" \
    --region "${AWS_REGION}" \
    --query 'services[0].[status,desiredCount,runningCount,pendingCount]' \
    --output text
}

ecs_counts() {
  aws_cli ecs describe-services \
    --cluster "${ECS_CLUSTER}" \
    --services "${ECS_SERVICE}" \
    --region "${AWS_REGION}" \
    --query 'services[0].[desiredCount,runningCount,pendingCount]' \
    --output text
}

rds_state() {
  aws_cli rds describe-db-instances \
    --db-instance-identifier "${RDS_INSTANCE}" \
    --region "${AWS_REGION}" \
    --query 'DBInstances[0].DBInstanceStatus' \
    --output text
}

is_rds_failure_state() {
  case "$1" in
    failed|incompatible-network|incompatible-option-group|incompatible-parameters|inaccessible-encryption-credentials|inaccessible-encryption-credentials-recoverable|restore-error|storage-full)
      return 0
      ;;
    *)
      return 1
      ;;
  esac
}

wait_for_rds_state() {
  local target_state="$1"
  local attempt current_state

  for ((attempt = 1; attempt <= RDS_WAIT_MAX_ATTEMPTS; attempt++)); do
    current_state="$(rds_state)"
    echo "RDS check ${attempt}/${RDS_WAIT_MAX_ATTEMPTS}: ${current_state} (waiting for ${target_state})"

    if [[ "${current_state}" == "${target_state}" ]]; then
      return 0
    fi

    if is_rds_failure_state "${current_state}"; then
      die "RDS entered failure state '${current_state}' while waiting for '${target_state}'."
    fi

    sleep_between_checks
  done

  die "RDS did not reach '${target_state}' after ${RDS_WAIT_MAX_ATTEMPTS} checks. Last state: '${current_state}'."
}

wait_for_ecs_counts() {
  local expected_desired="$1"
  local expected_running="$2"
  local attempt desired_count running_count pending_count

  for ((attempt = 1; attempt <= ECS_WAIT_MAX_ATTEMPTS; attempt++)); do
    read -r desired_count running_count pending_count <<< "$(ecs_counts)"
    echo "ECS check ${attempt}/${ECS_WAIT_MAX_ATTEMPTS}: desired=${desired_count}, running=${running_count}, pending=${pending_count}"

    if [[ "${desired_count}" == "${expected_desired}" && "${running_count}" == "${expected_running}" && "${pending_count}" == "0" ]]; then
      return 0
    fi

    sleep_between_checks
  done

  die "ECS did not reach desired=${expected_desired}, running=${expected_running}, pending=0."
}

show_status() {
  echo "ECS status / desired / running / pending: $(ecs_summary)"
  echo "RDS status: $(rds_state)"
}

stop_rds() {
  local current_state
  current_state="$(rds_state)"
  echo "Current RDS status before stop: ${current_state}"

  case "${current_state}" in
    stopped)
      echo "RDS is already stopped."
      return 0
      ;;
    stopping)
      wait_for_rds_state stopped
      return 0
      ;;
    available)
      ;;
    *)
      if is_rds_failure_state "${current_state}"; then
        die "Refusing to stop RDS from failure state '${current_state}'."
      fi
      echo "Waiting for RDS to become available before requesting a stop..."
      wait_for_rds_state available
      ;;
  esac

  aws_cli rds stop-db-instance \
    --db-instance-identifier "${RDS_INSTANCE}" \
    --region "${AWS_REGION}" \
    >/dev/null
  wait_for_rds_state stopped
}

start_rds() {
  local current_state
  current_state="$(rds_state)"
  echo "Current RDS status before start: ${current_state}"

  case "${current_state}" in
    available)
      echo "RDS is already available."
      return 0
      ;;
    starting)
      wait_for_rds_state available
      return 0
      ;;
    stopping)
      wait_for_rds_state stopped
      ;;
    stopped)
      ;;
    *)
      if is_rds_failure_state "${current_state}"; then
        die "Refusing to start RDS from failure state '${current_state}'."
      fi
      echo "Waiting for the already-running RDS instance to become available..."
      wait_for_rds_state available
      return 0
      ;;
  esac

  aws_cli rds start-db-instance \
    --db-instance-identifier "${RDS_INSTANCE}" \
    --region "${AWS_REGION}" \
    >/dev/null
  wait_for_rds_state available
}

stop_dev() {
  echo "Scaling ${ECS_CLUSTER}/${ECS_SERVICE} to zero tasks..."
  aws_cli ecs update-service \
    --cluster "${ECS_CLUSTER}" \
    --service "${ECS_SERVICE}" \
    --desired-count 0 \
    --region "${AWS_REGION}" \
    >/dev/null
  wait_for_ecs_counts 0 0

  stop_rds
  echo "DEV is stopped."
}

start_dev() {
  start_rds

  echo "Scaling ${ECS_CLUSTER}/${ECS_SERVICE} to one task..."
  aws_cli ecs update-service \
    --cluster "${ECS_CLUSTER}" \
    --service "${ECS_SERVICE}" \
    --desired-count 1 \
    --region "${AWS_REGION}" \
    >/dev/null
  wait_for_ecs_counts 1 1

  local attempt
  for ((attempt = 1; attempt <= HEALTH_WAIT_MAX_ATTEMPTS; attempt++)); do
    if "${CURL_COMMAND}" --fail --silent --show-error "${DEV_HEALTH_URL}" >/dev/null; then
      echo "DEV API is healthy: ${DEV_HEALTH_URL}"
      echo "DEV is started."
      return 0
    fi

    echo "DEV API health check ${attempt}/${HEALTH_WAIT_MAX_ATTEMPTS} did not pass yet."
    sleep_between_checks
  done

  die "DEV infrastructure started, but ${DEV_HEALTH_URL} did not become healthy."
}

validate_targets
show_status

case "${ACTION}" in
  status) exit 0 ;;
  stop) stop_dev ;;
  start) start_dev ;;
esac

show_status
