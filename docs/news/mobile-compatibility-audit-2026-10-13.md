# News CMS: mobile compatibility audit — October 13, 2026

Compared the complete feature diff with `origin/dev` and traced the unchanged iOS and Android clients into the shared API. This audit updates DEV PR #435; it does not merge or deploy it.

## Findings fixed

1. **Load balancer rate-limit collision.** Using only the socket address would count unrelated users behind the ALB together. The DEV and production profiles now explicitly trust the six existing ALB subnet ranges. Only a request from those peers can use the final IP appended by the ALB. Caller-supplied prefixes and X-Real-IP do not select the bucket. Invalid values fall back to the socket peer, and IPv6 spellings are normalized. Local/default configuration trusts no proxies.
2. **Administrator attempts consumed mobile login quota.** Administrator login/MFA now have a separate bucket and separate bounded counter map. Native login keeps its existing allowance. Administrator login and verification still share their own limit to prevent challenge retries bypassing it.
3. **New administrator timeouts changed the consumer SDK client.** Restored the consumer Cognito client's existing settings and made it the primary bean. News MFA and membership checks use an explicitly qualified client with its own connection pool and bounded timeouts. Neither mobile login/refresh nor account deletion inherits those news settings.

Read-only AWS verification: `sanctuary-api-alb` uses X-Forwarded-For **append** mode with client ports disabled. Its six subnet CIDRs are `172.30.0.0/24` through `172.30.5.0/24`. Both API service security groups permit port 8080 only from the ALB security group `sg-0931e5a7d1c0d69f8`; there are no public CIDR ingress rules on that port. No AWS settings were changed. If ALB subnets or forwarding mode change, update the trusted-proxy configuration and rerun these tests before deployment.

## Contract inspection

| Mobile behavior | Inspection result |
| --- | --- |
| Registration, confirmation, resend, login, password recovery/reset | `/auth` controllers, request/response DTOs and consumer Cognito service unchanged. Native login still uses USER_PASSWORD_AUTH with the environment's configured app client. |
| Session refresh | `/auth/refresh` and REFRESH_TOKEN_AUTH unchanged; refresh token retained in response. Both clients prefer ID tokens with access-token fallback. |
| Profile and preferences | `/me` and `/me/preferences` controllers, services and DTOs unchanged. Ordinary valid Cognito bearer tokens remain accepted without news membership or MFA. |
| Favorites and novena progress | Routes, serializers, repositories and tables unchanged. Existing mobile retry/refresh behavior unchanged. |
| Account deletion | Existing local deletion and Cognito calls unchanged. Scoped runtime IAM includes ListUsers, AdminDeleteUser and AdminUserGlobalSignOut on the environment's pool. |
| Saints, prayers, novenas, intentions, patronages, calendar | Native URL mappings match the existing public `/content` and `/calendar` controllers. Those controllers, DTOs, repositories and calendar logic have no feature diff. |
| New authorization and request limits | News security chain matches `/admin` only; news JSON filter matches `/admin/news` and `/auth/news` only. No administrator header/proof is required for `/me` or existing native `/auth` routes. Multipart limits affect news uploads; no existing native API accepts multipart files. |
| Web cookies | Consumer cookie issuance unchanged. Admin cookies are host-only and scoped to `/admin`; native bearer-header authentication takes precedence and does not depend on browser cookies. |
| Database migrations | V16–V19 create/change newsroom tables and indexes only. No existing account, preferences, favorites, progress, content or calendar table is altered. |
| Native code/builds | Zero changes under `apps/ios` or `apps/android`, including versions and configurations. Their workflow path filters do not match this PR. Desktop/web alone is 1.0.16. |
| Deployment | Existing API/web DEV workflows validate this PR. Opening/updating it does not deploy. A later DEV merge deploys the shared DEV API/web, with new roles, challenge secret and verified database transport TLS. It does not publish mobile binaries or change production/MAIN. |

## Verification

Final full API suite: **95 tests, 94 passed, 1 skipped, zero failures/errors**. The existing calendar database audit skips because its separate localhost:5432 fixture is unavailable; the isolated news PostgreSQL lifecycle tests ran on port 15439.

- Regression tests cover separate clients behind the ALB, forged prefixes, untrusted peers, malformed addresses, IPv6 aliases and separate administrator/consumer allowance.
- Configuration binding test verifies the explicit proxy ranges.
- SDK wiring test verifies the consumer remains primary, administrator uses a distinct client, and new timeouts apply only to that administrator client.
- `NativeApiCompatibilityTest` uses both actual security-chain configurations, real AuthController/UserProgressController HTTP and JSON handling, and RSA-signed tokens through the production JWT validator. It verifies ID/access-token profile access without administrator group/cookies/MFA; native favorite writes, novena list/removal and account deletion; login/refresh token responses without admin cookies; and rejection of absent/invalid bearer tokens. Service persistence/Cognito are mocked in this test, and no real account is deleted.
- Existing isolated PostgreSQL news lifecycle, MFA/security tests and consumer account/profile/calendar tests remain in the full API suite.

This inspection found and fixed concrete shared-backend regressions. It establishes that mobile contracts and data structures remain compatible; it is not a device test or a deployment test. Shared API dependency/container, IAM and JDBC transport changes still require normal DEV rollout verification before promotion. No mobile release is required for these fixes. No database instance, automated backup, snapshot schedule or encryption configuration was changed in this audit.
