> **Owner decision — October 13, 2026:** Keep the existing database setup simple. Use owner-managed manual backups only. Do not enable automated backups, schedule snapshots, change database encryption, or move/replace an RDS database instance without a new explicit instruction. The two existing manual snapshots may remain. AWS verification: DEV and production RDS retention are both 0, no pending RDS changes, and no active AWS Backup plans in us-east-1. The earlier maintenance-window proposal is withdrawn; it is not a pending approval or release requirement under the current scope.

# Security hardening results — October 13, 2026

Implementation: codex/sanctuary-news-fresh-start, prepared for a DEV pull request. Desktop/web 1.0.16; iOS/Android unchanged at 1.0.15. PR checks use only the dedicated DEV workflows and DEV Environment Control validation; production/MAIN and mobile workflows are excluded. Opening the PR does not deploy the app.

## Implemented

- Entire admin namespace fails closed; future app-management APIs cannot inherit news-admin access.
- Exact-client/token-type admin identity validation, real signature/issuer/time validation, live Cognito account/group/TOTP checks, and permanent proof revocation after access removal.
- Encrypted, expiring, atomically consumed shared MFA challenges; shared account login throttling; bounded request/key storage; forwarding-header spoofing fix.
- Host-only HttpOnly admin cookies, hostile-origin and mutation-header enforcement, pre-deserialization JSON size limits, and redacted validation/auth state.
- Early production configuration validation before database/Flyway initialization, verified PostgreSQL TLS with AWS's public RDS CA bundle, strict production web script policy, and prepared framing/HSTS headers.
- Patched Java/npm dependencies, repeatable advisory gate with exact-version/time-limited reviewed exceptions, PostgreSQL integration checks in existing CI, non-root/pinned container images, removal of unused supervisor from all final image layers, and excluded secrets/test files from Docker build context.
- Private local backup, verified restore of both existing articles to a persistent local volume, and restarted local API on the final code.

## Validation

| Check | Result |
| --- | --- |
| API suite, PostgreSQL enabled | 95 total: 94 passed, 1 skipped, 0 failures/errors |
| Frontend | 31 passed across 8 files |
| Production web build | Passed; CSP applied; no inline executable scripts/event handlers |
| npm audit | 0 reported vulnerabilities |
| OSV, resolved Java runtime/test dependencies | 135 packages; 0 actionable findings; 2 reviewed non-applicable advisories |
| Final Docker Scout image scan | 301 packages; 2 Spring MVC advisories in one package; no OS/supervisor findings |
| Container local and production-profile startup | Health 200; production-profile anonymous admin 401; disabled-auth admin denied |
| Unsafe production startup | Refused before database initialization |
| Local real public API | 200, both preserved articles |
| New IAM task roles | Own-pool administrator reads allowed; DEV access to production pool denied |
| Infrastructure template / workflow syntax | CloudFormation accepted; four workflow YAML files parsed |
| Whitespace / mobile source diff | Clean; no mobile changes |

The two advisories concern XSLT view rendering and SSE view fragments, neither used by this REST API. Their documented assessments expire November 13, 2026; the gate fails if affected features are introduced. They are not represented as patched or zero vulnerabilities. See [assessments](../security/api-advisory-exceptions.json) and primary-source links in the runbook.

HTTP lifecycle tests use real Spring filters/controllers, PostgreSQL transactions, cookies, and image processing, with Cognito and JWT decoding substituted. Separate tests use real RSA signatures and the production JWT validator, including consumer ID/access tokens used by the unchanged native sign-in contract. Actual deployed owner MFA/publication acceptance remains a release check after DEV deployment. DEV account state was read from Cognito: Outlook is confirmed/enabled with software-token MFA and news-admin; enrollment was not reset.

## Cloud preparation completed

- sanctuary-news-auth-dev and sanctuary-news-auth-prod reached CREATE_COMPLETE.
- Separate environment-scoped API task/execution roles, private challenge keys, and unassigned web security-header policies are provisioned.
- Four metadata variables per GitHub environment were configured. Existing deployment roles received exact-resource PassRole/CloudFront prerequisites; no workflow was triggered.
- No ECS task, CloudFront distribution, Cognito identity/MFA setting, or live database endpoint/configuration was changed.
- Production safety snapshot sanctuary-prod-security-20261013 is available. Encrypted backup copy sanctuary-prod-security-encrypted-20261013 is available and reports Encrypted=true.

## Database scope confirmed

The owner chose the existing database setup and manual backups. No maintenance-window approval is pending. DEV and production RDS retention were verified as zero, with no pending RDS modifications or AWS Backup plans in us-east-1. No backup scheduling, storage-encryption change, or database cutover is included in the DEV PR. The two existing manual safety snapshots may remain.

The [production security runbook](production-security-runbook.md) records the prepared roles/settings and the remaining administrator/deployed acceptance steps.

## Mobile compatibility follow-up

The [mobile compatibility audit](mobile-compatibility-audit-2026-10-13.md) identified and fixed ALB-wide rate-limit collisions, shared administrator/consumer login buckets, and administrator SDK timeouts affecting the consumer client. Added actual native-controller/security-chain tests with RSA-signed tokens and SDK wiring/configuration tests. Consumer endpoint DTOs/services/tables and native source/version/workflows remain unchanged. AWS inspection was read-only; this follow-up adds no cloud resources.
