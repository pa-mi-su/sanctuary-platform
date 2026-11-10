# Sanctuary content and account administration

Implementation branch: `codex/content-admin`, based on `main` at `5cf491f`.

The Angular application serves `/admin`. The static build emits a dedicated extensionless `admin` HTML object and the web deploy workflows upload it with an HTML content type and `no-store`. The Angular admin code is lazy-loaded. All private operations use `/admin-api/**` on the existing Spring API. There is no mobile admin UI or native source change.

## INSTEAD audit

Read-only source review of `/Users/pms/repos/instead/docs/admin-panel.md`, `docs/admin-auth-security-review.md`, and the admin authentication helpers, request/session handling in `website/admin/admin.mjs`, and authorization functions in `supabase/admin-panel.sql`. This was not a live Supabase or AWS penetration test.

INSTEAD's useful patterns are verified identity plus server-side membership, TOTP before private access, paginated lists, content editing that preserves IDs, reasons and before/after audit history, and session cleanup. Its moderation queues, recipe reports, account deletion, and multi-admin grants address a different product. Sanctuary has one fixed owner, no admin-grant API, and read-only user inspection. Sanctuary uses Cognito and PostgreSQL; it does not copy Supabase credentials or reuse INSTEAD's auth session.

## Content

Collections: prayers (including rosary), saints, novenas, novena days, intention terms, and patronage terms. Edit English, Spanish, and Polish fields and HTTPS image URLs. Tags, intention links, and saint patronage links are editable. Normalized novena intention links synchronize the legacy localized intention rows used by calendar/detail queries when saved through this panel. Existing IDs, slugs, and novena day numbering cannot change.

Publishing requires a change reason and explicit review checkbox. The API validates allowed collections/columns, relationships, numeric fields, feast dates, and identifiers. A hash of the current row and relationships rejects stale saves. Changes and audit snapshots commit in one transaction. Publishing history shows before/after snapshots; restore currently means making a reviewed new edit using the old values, rather than a one-click rollback.

New novenas require all nine days in the creation form, including an English prayer for each day. The parent, days, relationships, and audit records publish in one transaction. Existing day text can be edited in Novena days; existing day numbering cannot be changed. Saving publishes immediately; there is no draft, scheduled-publish, or archive/delete workflow in this version. Saint source lists, search aliases, and liturgical serving rules remain outside this editor. Images are referenced by HTTPS URL; there is no upload service.

Content APIs read the database, so refreshed clients see changes without a native release. Already loaded screens do not receive live pushes. Open Graph previews are generated during web builds and may retain old copy until the next web deployment.

## User administration and activity definitions

- All registrations reads the existing public-app Cognito pool with cursor pagination. It includes unconfirmed users and accounts without a database profile. It is deliberately separate from the profile count.
- Users & activity searches database profiles by email/name, optionally active within seven days. It shows favorites with content titles, current novena commitments/status, completed days, recorded login count and events. Each list is paginated at 20 rows.
- `last_login_at`, `login_count`, and `login` events are recorded only after successful password login through the existing native/web API. Token refresh is not counted as a login. Provider responses are the source of identity; a client cannot submit an arbitrary subject to the recorder. A telemetry write failure does not prevent successful login and emits a generic server warning.
- `last_seen_at` records authenticated account/profile requests; it does not track anonymous browsing or indicate online presence. The active 24-hour/7-day/30-day counts use this value. Deployment does not mark all historical accounts active.
- The old `last_sign_in_at` field is refreshed by profile upserts and had a historical migration backfill. It is not shown as actual login history.
- PostgreSQL triggers record saved favorites and changes to novena status/completed days. They apply to all clients without mobile code changes. Current saved items and commitments already exist before deployment; past events/login counts cannot be reconstructed.
- Commitments represent current retained state, not every past run of a novena. Removing a commitment can remove it from the current list; already recorded progress events remain until account deletion. Favorite removal and commitment removal do not currently generate dedicated events.
- User profile/favorite/progress/activity rows cascade on account deletion. No user data is added to content audit snapshots. Provider logs/backups retain their existing policies.

## Authentication and session boundaries

Only verified email `pmsulikowski@icloud.com` AND the configured immutable Cognito subject are accepted. Matching an email alone, public signup, a regular Cognito token, or a browser flag cannot grant admin access. Set the subject from an independently verified operator lookup; never auto-discover and grant on login.

Use a dedicated admin Cognito app client **in a separate admin-only user pool in the same AWS region as the API's current Cognito pool**. Disable self signup and create only the owner account. This keeps owner MFA enrollment separate from the public clients, which do not implement Cognito MFA challenges. `SANCTUARY_ADMIN_CLIENT_ID` selects that client; the registration directory continues using the public pool configuration. The admin client must have no client secret and allow `ALLOW_USER_PASSWORD_AUTH`. Enable software-token MFA in the admin pool; optional MFA allows the implemented password-authenticated first-enrollment flow. Required MFA is also supported through Cognito's `MFA_SETUP` challenge. The panel handles the invitation’s temporary password, asks the owner to choose a permanent password, and then requires authenticator enrollment before private access.

Provider challenge sessions and temporary enrollment access tokens are AES-256-GCM encrypted in PostgreSQL with an external 32-byte Base64 key. Challenge identifiers and admin session tokens are random 256-bit values stored only as SHA-256 hashes. Challenges expire after five minutes and allow at most five verification attempts. Existing verified software factors cannot be replaced through the password-only enrollment path. An opaque admin session is issued only after successful TOTP verification and a live Cognito owner attribute check. Challenges are consumed once, including concurrent verification requests.

Admin sessions expire after eight hours or 15 idle minutes, enforced on every private API call using PostgreSQL time. The browser clears private state after inactivity and on sign-out; late responses cannot restore the ended session. Admin tokens remain only in component memory, never in localStorage, sessionStorage, URLs, or public-app cookies. Reload requires sign-in again. A server filter protects every admin route (including future routes), independently of the public JWT chain. Admin is unavailable when disabled or the public API auth switch is off. Every request requires a custom header and all private requests an explicit bearer token; ambient cookies are ignored. The existing exact-origin CORS configuration applies. Admin responses use `Cache-Control: no-store`.

Login attempts have shared PostgreSQL owner/IP/global limits plus Cognito's own limits. `getRemoteAddr` is used without trusting client-supplied forwarding headers; behind a proxy the IP bucket may be shared, and the global/owner buckets remain effective. This favors a single-owner deployment rather than general-purpose account login.

Revocation: sign-out deletes that session. To revoke all access, disable admin in deployment configuration or delete its `admin_sessions` rows through independently secured operations. Changing the pinned subject also rejects existing sessions. Disabling a Cognito account alone does not immediately revoke these separate server sessions. Lost MFA recovery is an operator process in the separately secured Cognito console; there is no email-only bypass or public factor-reset endpoint.

## Production activation

Code and CI preparation do not constitute a production deployment. No live owner grant, user inspection, credential creation, MFA enrollment, or database migration was performed during implementation.

1. Provision the separate admin-only Cognito pool/client in the API's region. Confirm the owner's verified email and immutable subject privately. Enable TOTP, disable self signup, and let the owner finish permanent-password setup. Do not share passwords, setup keys, or OTPs in chat or logs.
2. Store a cryptographically random 32-byte Base64 key as a **plain string secret** in AWS Secrets Manager. Grant the ECS task execution role `secretsmanager:GetSecretValue` on only this secret (and KMS decrypt if required). Do not put the encryption key in GitHub variables or task-definition environment values.
3. Add GitHub environment variables `API_PROD_ADMIN_ENABLED=true`, `API_PROD_ADMIN_OWNER_SUB`, `API_PROD_ADMIN_CLIENT_ID`, and `API_PROD_ADMIN_KEY_SECRET_ARN`. DEV uses matching `API_DEV_...` names and separate credentials/key. Missing activation configuration leaves admin disabled by default. Keep the existing public API authentication enabled.
4. Ensure the API task role can call `cognito-idp:ListUsers` on the public-app pool for the registration directory. Admin user-based Cognito calls use the dedicated client's authentication flow/access token. No service-role token is delivered to Angular.
5. Review and deploy API first through the existing promotion process, then web. Flyway applies V20/V21. Check the real deployment's schema history before release, especially if another feature branch has deployed additional migrations. The task-definition renderer explicitly carries the new settings and secret reference; it otherwise replaces the environment/secrets arrays.
6. Verify `/admin` is HTML with no-store and noindex metadata. Verify anonymous and ordinary-user admin calls fail, private replies are no-store, the owner can enroll and sign in, a wrong MFA code fails, and sign-out invalidates the old token. Test in DEV with synthetic content first. The owner must perform real authenticator enrollment privately.

Encryption-key rotation invalidates pending challenges; it does not revoke existing opaque admin sessions. To revoke those as well, clear `admin_sessions`. Provider sessions/access tokens are never stored in plaintext or exposed to the browser.

## Pipelines and verification

Existing native path filters remain unchanged. Angular changes select web workflows; Java/migration changes select API workflows. Neither selects native builds. The root lockfile also does not select native builds. Native source or direct native workflow changes still select their respective validation paths.

Web PR/deployment jobs now run unit tests before builds. API PR/deployment jobs run their tests against a disposable PostgreSQL 17 service, including real migration/MFA/session/content/user-isolation tests. The Ruby path-isolation check runs in web/API PR validation and checks representative positive and negative cases. Workflow-only and test-script-only pushes do not deploy app artifacts.

Local commands:

```sh
ruby .github/scripts/test-workflow-path-isolation.rb
npm test --workspace web -- --watch=false
# In apps/web:
npx ng build --configuration production
# Disposable local/CI PostgreSQL only; credentials sanctuary_test / sanctuary_test:
ADMIN_TEST_DB_URL=jdbc:postgresql://127.0.0.1:5432/sanctuary_admin_test mvn -f apps/api/pom.xml clean test
```

The database integration tests refuse non-local URLs and skip unless `ADMIN_TEST_DB_URL` is provided. They are required in the edited API pipelines. Local clean builds matter: stale ignored `target/classes` from another feature branch can contain incompatible Flyway migration resources.

Verified locally: 63 API tests passed (one pre-existing test skipped), 15 web tests passed, four workflow checks passed with 54 assertions, and the full production web build generated 628 route objects including `/admin`. The build reports a 565 kB initial-bundle warning against the 500 kB warning threshold, below the 1 MB failure limit. Local database tests used PostgreSQL 15; CI is configured for PostgreSQL 17. Live deployment and MFA enrollment remain unverified.


## DEV rollout preparation — November 10, 2026

Provisioned the isolated `sanctuary-dev-content-admin` pool/client, enabled software-token MFA, disabled self registration, pinned the single requested owner, and configured the DEV GitHub environment. The provider emailed the temporary password directly to the owner; no password or MFA secret was read or recorded by the agent. Added least-privilege directory-read and challenge-key-read policies to the existing DEV task/execution roles.

DEV's previous image contains news migrations V16–V19 absent from current main. Those exact migration files are preserved from deployed commit `b8b5d346e91e2bbc6ec561a43a0794e65fe5648b`; content-admin migrations now use V20/V21. This preserves schema history without reintroducing the experimental news app. The panel now handles `NEW_PASSWORD_REQUIRED` before MFA enrollment, and neither password setup nor the invitation challenge can issue an admin session.

Production provisioning was rejected by automatic approval review as outside the stated DEV scope. No production identity, IAM, secret, or GitHub configuration changes were made. Production activation requires explicit approval after DEV verification.
