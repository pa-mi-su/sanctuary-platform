#!/usr/bin/env bash
set -euo pipefail
umask 077
: "${WEB_CLOUDFRONT_DISTRIBUTION_ID:?Missing distribution ID}"
: "${WEB_SECURITY_HEADERS_POLICY_ID:?Missing response headers policy ID}"
: "${WEB_DEPLOY_ENVIRONMENT:?Missing deployment environment}"
case "${WEB_DEPLOY_ENVIRONMENT}" in
  dev) expected_alias=dev.mydailysanctuary.com ;;
  prod) expected_alias=mydailysanctuary.com ;;
  *) echo 'Invalid deployment environment' >&2; exit 1 ;;
esac
task_temp_dir="$(mktemp -d)"
trap 'rm -rf "${task_temp_dir}"' EXIT
# AWS's fixed managed policy works on the Free plan; do not silently fall back
# from arbitrary custom policies or change the distribution's pricing plan.
readonly managed_security_policy=67f7725c-6f97-4210-82d7-5512b31e9d03
if [[ "${WEB_SECURITY_HEADERS_POLICY_ID}" != "${managed_security_policy}" ]]; then
  aws cloudfront get-response-headers-policy --id "${WEB_SECURITY_HEADERS_POLICY_ID}" > "${task_temp_dir}/policy.json"
  if [[ "$(jq -r '.ResponseHeadersPolicy.ResponseHeadersPolicyConfig.Name' "${task_temp_dir}/policy.json")" != "sanctuary-${WEB_DEPLOY_ENVIRONMENT}-web-security" ]]; then
    echo 'Security policy does not belong to this environment' >&2; exit 1
  fi
fi
aws cloudfront get-distribution-config --id "${WEB_CLOUDFRONT_DISTRIBUTION_ID}" > "${task_temp_dir}/current.json"
jq -e --arg alias "${expected_alias}" '.DistributionConfig.Aliases.Items | index($alias) != null' "${task_temp_dir}/current.json" >/dev/null
if [[ "${WEB_DEPLOY_ENVIRONMENT}" == dev ]]; then
  jq -e '.DistributionConfig.Aliases.Items | index("mydailysanctuary.com") == null' "${task_temp_dir}/current.json" >/dev/null
else
  jq -e '.DistributionConfig.Aliases.Items | index("dev.mydailysanctuary.com") == null' "${task_temp_dir}/current.json" >/dev/null
fi
jq --arg policy "${WEB_SECURITY_HEADERS_POLICY_ID}" '
  .DistributionConfig
  | .DefaultCacheBehavior.ResponseHeadersPolicyId = $policy
  | if .CacheBehaviors.Quantity > 0 then .CacheBehaviors.Items |= map(.ResponseHeadersPolicyId = $policy) else . end
' "${task_temp_dir}/current.json" > "${task_temp_dir}/updated.json"
if jq -e --slurpfile next "${task_temp_dir}/updated.json" '.DistributionConfig == $next[0]' "${task_temp_dir}/current.json" >/dev/null; then
  echo 'Web security headers already configured.'
else
  aws cloudfront update-distribution --id "${WEB_CLOUDFRONT_DISTRIBUTION_ID}" \
    --if-match "$(jq -r '.ETag' "${task_temp_dir}/current.json")" \
    --distribution-config "file://${task_temp_dir}/updated.json" --query 'Distribution.Id' --output text
  aws cloudfront wait distribution-deployed --id "${WEB_CLOUDFRONT_DISTRIBUTION_ID}"
fi
