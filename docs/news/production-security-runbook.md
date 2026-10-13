> **Owner decision — October 13, 2026:** Keep the existing database setup simple. Use owner-managed manual backups only. Do not enable automated backups, schedule snapshots, change database encryption, or move/replace an RDS database instance without a new explicit instruction. The two existing manual snapshots may remain. AWS verification: DEV and production RDS retention are both 0, no pending RDS changes, and no active AWS Backup plans in us-east-1. The earlier maintenance-window proposal is withdrawn; it is not a pending approval or release requirement under the current scope.

# News production security release — October 13, 2026

## Completed in the feature branch

- Administrator identity requires a verified Cognito ID token for the configured client, a current news-admin role, a confirmed/enabled account, enrolled software-token MFA, and a token-bound server proof. Password-only sessions and consumer cookies cannot create administrator access.
- Every protected request rechecks the account and group in Cognito. Failed checks permanently remove that proof, including fail-closed behavior during Cognito outages. Re-adding a role does not resurrect a revoked proof.
- MFA challenges are AES-256-GCM encrypted in PostgreSQL, expire after five minutes, have authenticated challenge IDs, and are consumed atomically across replicas. Five incorrect code attempts end a challenge. No browser-persistent tokens, QR service, or stored passwords are used.
- Login throttling is shared per normalized account. Auth request buffers, challenge/limiter tables, headers in logs, and JSON body sizes are bounded. Forwarded IP headers are ignored unless the socket peer matches the explicitly configured ALB subnet ranges. For those peers only the final ALB-appended literal address supplies the key; caller-supplied prefixes are ignored. Administrator counters are isolated from consumer login counters. These IP counters are process-local; administrator per-account challenge/login limits are shared in PostgreSQL.
- Production checks run immediately after config loading, before DataSource/Flyway initialization, and reject disabled auth, insecure cookies, invalid/wildcard/non-HTTPS CORS origins, missing Cognito configuration, missing challenge encryption key, and unverified PostgreSQL TLS. Administrator cookies are HttpOnly and host-only.
- API image pins both build/runtime image digests and uses the current Ubuntu 26.04 LTS Java 21 runtime and runs as UID/GID 10001. The inherited /usr/bin/pebble Go supervisor is removed because this image starts Java directly. The final stage copies the cleaned runtime filesystem so that deleted binaries are absent from distributed lower layers too. Testing the older Noble variant introduced additional OS advisories, so the final image retains the current LTS base while removing the unused affected binary. The public AWS RDS us-east-1 root CA bundle is included; deployed JDBC URLs use sslmode=verify-full and sslrootcert=/app/rds-ca.pem. Refresh this public certificate bundle as part of certificate maintenance.
- Production web builds include a strict script CSP. Critical CSS inlining is disabled to avoid CSP-blocked onload handlers. Build fails on inline scripts/event handlers. Angular article content remains text, not executable HTML.
- Existing API CI definitions now run isolated PostgreSQL integration tests and OSV scanning. No new workflow was dispatched, and MAIN was not changed. Desktop/web is 1.0.16; mobile sources and versions are unchanged.

## Cloud inspection and preparation status

Read-only AWS inspection on October 13 found:

| Resource | Observed state | Required action |
| --- | --- | --- |
| sanctuary-prod-db | PostgreSQL 17.9, db.t4g.micro, 20 GB, single AZ | Keep sizing unless separately changed |
| Automated backups | Retention 0 days | Enable at least seven days; verify a recovery point and restore |
| Storage encryption | false | Migrate to encrypted storage before publishing private newsroom data |
| Deletion protection | true | Preserve protection on replacement |
| Network | Public endpoint; no worldwide 5432 rule, but a temporary laptop /32 and default-VPC group remain | Replacement private; allow only API security group, remove bootstrap ingress after validation |
| API roles, DEV and production | Running services still use ecsTaskExecutionRole; separate scoped roles are now provisioned | Switch on approved code release |
| News challenge secrets | Initially absent; now provisioned in DEV and production | ECS injection is prepared for release |
| Administrator | DEV Outlook confirmed, enabled, TOTP enrolled, news-admin | Production Outlook provisioning/enrollment still required; do not remove existing owner access until replacement verified |

Production database security group: sg-0c6f3f32e3803bc03. API-to-PostgreSQL ingress source: sg-000f4021f05813471. Do not infer that a public RDS endpoint means unrestricted internet access: the inspected rule was restricted to the listed groups and one /32.

## Prepared infrastructure

Both CloudFormation stacks sanctuary-news-auth-dev and sanctuary-news-auth-prod reached CREATE_COMPLETE. The four output identifiers per environment were stored in GitHub environment variables. A narrowly scoped sanctuary-news-release-prerequisites policy was added to each existing GitHub deployment role for the exact new PassRole targets and its own web distribution/policy. Trust remains restricted to this repository's matching environment. These changes did not dispatch any workflow, update an ECS service, attach a CloudFront policy, or change Cognito identities.

A manual production safety snapshot sanctuary-prod-security-20261013 completed before any database settings/cutover changes. An encrypted copy sanctuary-prod-security-encrypted-20261013 is also available and reports Encrypted=true; neither snapshot is an automated backup policy or a cutover-current replica.


`infra/news-admin/template.yml` was accepted by AWS CloudFormation validate-template. It creates a CloudFront web security headers policy, an environment-specific Secrets Manager key, a Cognito-only application task role, and an execution role that reads only that environment's database/challenge secrets plus standard ECS image/log permissions. It does not update ECS, RDS, Cognito membership, or MFA configuration.

Use separate stacks, for example sanctuary-news-auth-dev and sanctuary-news-auth-prod, with EnvironmentName, UserPoolId, and the actual DatabaseSecretArn. The key is generated by Secrets Manager as 64 hexadecimal characters and is never passed through GitHub or printed. Keep retained keys under the same access controls as database credentials. No automatic key rotation is configured; rotate with coordinated task replacement, knowing pending challenges will expire while completed proofs remain separately revocable.

Map stack outputs to existing GitHub environment variables:

| GitHub variable | DEV / production value |
| --- | --- |
| API_DEV_NEWS_CHALLENGE_SECRET_ARN / API_PROD_NEWS_CHALLENGE_SECRET_ARN | ChallengeSecretArn |
| API_DEV_TASK_ROLE_ARN / API_PROD_TASK_ROLE_ARN | TaskRoleArn |
| API_DEV_EXECUTION_ROLE_ARN / API_PROD_EXECUTION_ROLE_ARN | ExecutionRoleArn |

Map WebSecurityHeadersPolicyId to WEB_DEV_SECURITY_HEADERS_POLICY_ID / WEB_PROD_SECURITY_HEADERS_POLICY_ID. The guarded web deployment script checks the environment policy name and target domain before changing response-header policy references. It preserves other distribution configuration and uses the current ETag. The web deploy role needs cloudfront:GetDistributionConfig and cloudfront:UpdateDistribution on the exact distribution, plus cloudfront:GetResponseHeadersPolicy on its exact policy. Attaching the prepared policy adds frame-ancestors, DENY framing, HSTS, and the CSP; no distribution was changed during this work.

The deployment role needs iam:PassRole only for these new exact role ARNs, with iam:PassedToService=ecs-tasks.amazonaws.com. Do not expand the application task role to read secrets, ECR, or deployment resources. If the RDS secret uses a customer-managed KMS key, add narrowly scoped kms:Decrypt to the execution role for that key, constrained to Secrets Manager.

The updated workflow files stop before deployment if these roles/secrets are missing. They inject the challenge key through ECS secrets and apply verified TLS to the JDBC URL. Do not bypass those preflights. A separate workflow smoke-test key is generated ephemerally; it is never the real environment key.

## Database scope — owner decision

Keep the existing databases and owner-managed manual backup process. Do not schedule snapshots, enable automatic backups, change storage encryption, or migrate the database. The earlier maintenance-window proposal is withdrawn. RDS retention was verified as zero in DEV and production, with no pending RDS changes or AWS Backup plans in us-east-1. Existing manual safety snapshots may remain.

Verified TLS in the API protects connections to the existing database; it does not alter database storage encryption or backup configuration. Flyway V16–V19 add the newsroom's application tables and access state through the normal API release, without creating or replacing an RDS instance.

## Dependency assessments

The permanent OSV gate scans resolved runtime and test/build dependencies and fails on network/incomplete-response errors or unreviewed findings. Two exact-version Spring MVC advisories remain classified as not applicable because this API has neither SSE fragment rendering nor XSLT view rendering. The assessments expire November 13, 2026, and fail if affected symbols appear in main source. They are recorded in docs/security/api-advisory-exceptions.json, not hidden as fixed vulnerabilities. Spring's 6.2 fix is enterprise-only; an OSS Spring Boot 4 migration is separate work if these features become necessary.

Primary sources: [SSE fragments advisory](https://spring.io/security/cve-2026-47890/), [XSLT view advisory](https://spring.io/security/cve-2026-47884/).

## Local data and acceptance

The two existing articles were backed up (backups/news-before-security-hardening-20261013.dump), restored into a new persistent Docker volume sanctuary-news-cms-data, and verified before switching the local API to port 15440. The old isolated test container on 15439 was preserved. Both backup and .env.news.local are Git-ignored; the local challenge key file has private permissions. Keep that key with local recovery material. A Docker volume survives container replacement, not disk loss: retain backups independently.

The public local API returns 200 and both existing articles; anonymous /admin/news returns 401. Existing admin sessions must sign in again after V19. The Outlook DEV authenticator remains enrolled. No owner credentials or current OTP were collected, and no real article was modified by automated tests.

## Remaining release acceptance

After the approved cloud preparation and DEV deployment, verify the actual task role, Secrets Manager injection, verified RDS TLS, Cognito challenge continuation, cookie/CORS headers, live role revocation, and owner publishing flow. Publish only after production administrator enrollment and deployed acceptance of the security controls above. The prepared CloudFront policy and deployment step must be applied and verified; meta CSP alone cannot enforce frame-ancestors. Firebase/app-management remains separate future work.

## Shared mobile API compatibility

See [mobile compatibility audit](mobile-compatibility-audit-2026-10-13.md) for the endpoint review and regression fixes. DEV/production trust only the existing ALB subnet ranges for the final appended client address; keep those ranges aligned with the ALB. Administrator limits and SDK connections/timeouts are separated from consumer authentication. No mobile source, version or workflow changes are included.
