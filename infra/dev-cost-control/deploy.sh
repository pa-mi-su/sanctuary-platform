#!/usr/bin/env bash

set -euo pipefail

readonly EXPECTED_ACCOUNT_ID="160885294528"
readonly EXPECTED_REGION="us-east-1"
readonly STACK_NAME="sanctuary-dev-cost-control"
readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly TEMPLATE_FILE="${SCRIPT_DIR}/template.yml"
readonly ACTION="${1:-}"

die() {
  echo "ERROR: $*" >&2
  exit 1
}

case "${ACTION}" in
  validate|deploy-disabled|deploy-enabled|outputs) ;;
  *) die "Usage: $0 {validate|deploy-disabled|deploy-enabled|outputs}" ;;
esac

command -v aws >/dev/null 2>&1 || die "AWS CLI is required."

actual_account_id="$(
  aws sts get-caller-identity \
    --region "${EXPECTED_REGION}" \
    --query Account \
    --output text
)"
[[ "${actual_account_id}" == "${EXPECTED_ACCOUNT_ID}" ]] || \
  die "Refusing AWS account '${actual_account_id}'; expected '${EXPECTED_ACCOUNT_ID}'."

case "${ACTION}" in
  validate)
    aws cloudformation validate-template \
      --region "${EXPECTED_REGION}" \
      --template-body "file://${TEMPLATE_FILE}"
    ;;
  deploy-disabled|deploy-enabled)
    if [[ "${ACTION}" == "deploy-enabled" ]]; then
      schedule_state="ENABLED"
    else
      schedule_state="DISABLED"
    fi

    aws cloudformation deploy \
      --region "${EXPECTED_REGION}" \
      --stack-name "${STACK_NAME}" \
      --template-file "${TEMPLATE_FILE}" \
      --parameter-overrides "ScheduleState=${schedule_state}" \
      --capabilities CAPABILITY_NAMED_IAM \
      --no-fail-on-empty-changeset \
      --tags Application=Sanctuary Environment=dev Purpose=cost-control

    aws cloudformation describe-stacks \
      --region "${EXPECTED_REGION}" \
      --stack-name "${STACK_NAME}" \
      --query 'Stacks[0].Outputs' \
      --output table
    ;;
  outputs)
    aws cloudformation describe-stacks \
      --region "${EXPECTED_REGION}" \
      --stack-name "${STACK_NAME}" \
      --query 'Stacks[0].[StackStatus,Parameters,Outputs]' \
      --output json
    ;;
esac
