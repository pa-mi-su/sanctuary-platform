#!/usr/bin/env bash
# S3 REST origins serve exact object keys, including trailing slashes.
# Publish the public app shell; article data and admin APIs remain authenticated separately.
set -euo pipefail
: "${WEB_S3_BUCKET:?Missing website bucket}"
: "${WEB_DEPLOY_ENVIRONMENT:?Missing deployment environment}"
case "${WEB_DEPLOY_ENVIRONMENT}" in
  dev) expected_bucket=dev.mydailysanctuary.com ;;
  prod) expected_bucket=mydailysanctuary.com ;;
  *) echo 'Invalid deployment environment' >&2; exit 1 ;;
esac
[[ "${WEB_S3_BUCKET}" == "${expected_bucket}" ]] || { echo 'Website bucket does not match deployment environment' >&2; exit 1; }
app_shell="${WEB_BUILD_DIR:-apps/web/dist/web/browser}/index.html"
[[ -s "${app_shell}" ]] || { echo 'Built app shell is missing or empty' >&2; exit 1; }
for route in news news/ admin admin/ admin/news admin/news/; do
  aws s3api put-object --bucket "${WEB_S3_BUCKET}" --key "${route}" \
    --body "${app_shell}" --content-type 'text/html; charset=utf-8' \
    --cache-control 'no-cache' --query ETag --output text
done
