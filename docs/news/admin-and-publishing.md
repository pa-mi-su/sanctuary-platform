# Sanctuary administration and publishing

Implementation branch: `codex/sanctuary-news-fresh-start`. No deployment is part of this work.

## Shared administration, separate permissions

Use one `/admin` entry point and navigation for distinct modules:

| Module | Frontend | Backend | Permission | State |
| --- | --- | --- | --- | --- |
| Newsroom | `/admin/news` | `/admin/news/**` | Cognito `news-admin` | Implemented |
| App management | `/admin/apps` | `/admin/apps/**` | Cognito `app-admin` | Planned |
| Notification sending | Inside App management | Dedicated notification endpoints | `notifications-admin` | Planned |

Initially `/admin` opens the protected newsroom. App management is labeled planned; it has no operational controls. When App management is implemented, make `/admin` a module chooser based on permissions returned by a protected server endpoint. Never infer permission from client storage or an email typed into a form. Each API module must enforce its own Cognito group. Membership in `news-admin` must not authorize app administration or notification sending. A person may explicitly belong to multiple groups.

## Newsroom

Sign in with the separate administrator account and authenticator. HttpOnly administrator cookies are scoped to /admin and do not use consumer token refresh. The API requires a verified client-bound Cognito ID token, news-admin membership, and a server proof of completed TOTP. Every protected request rechecks current account/group/MFA state. It remains denied when authentication is disabled. Mutations also require X-Sanctuary-News-Admin: 1 and trusted credentialed CORS origins. Administrator responses are not cacheable.

Create an article with a headline, summary, plain-text body, author, category (Vatican/U.S. Church), sources, and optional photo. Upload JPEG/PNG only, <=5 MB, <=6000 pixels per dimension and <=20 MP. The server decodes and re-encodes the image to strip metadata and reject active content. Give published images an accessible description and a credit/permission. Draft image requests are protected; public image access is available only while a published article references the image.

Use Draft, Published, or Scheduled status. Saving Published publishes immediately. Scheduled articles are selected by publication time on each public API read, with no worker dependency. Return an article to Draft to unpublish it. One article can be marked as the homepage lead. The version field rejects stale saves (409) rather than overwriting newer edits. Copies of articles are not currently archived; save audit records identify the actor, action, time, and resulting version. Correction notes are appended permanently and shown on public articles. There is no hard-delete control.

Public `/news` now reads published records from `/content/news`. It never falls back to fictitious stories on backend errors. `/news?preview=1` explicitly enables the original three-story design mockup. HTML is rendered as text; only HTTPS source URLs become links.

Application schema migrations: V16–V19 (articles/images/audit, MFA proofs, placement/trash, and shared protected sign-in state). Photos are initially stored in PostgreSQL (durable across application restarts); this intentionally avoids configuring another cloud service for a small newsroom. At greater volume, introduce a storage interface backed by private S3 with controlled public delivery and migrate existing image IDs. Retain byte/type/dimension validation, draft access restrictions, backups, and lifecycle cleanup for abandoned uploads.

## Cognito setup

AWS account verified: `160885294528`, region `us-east-1`.

- DEV: `sanctuary-dev-users`, `us-east-1_oCrXzjKbV`.
- Production: `sanctuary-prod-users`, `us-east-1_syYJKg0NY`.
- UAT currently uses production API/accounts; it does not have an independent Cognito pool.

The owner approved news-admin in both pools. The separate Outlook administrator is confirmed, enabled, and TOTP-enrolled in DEV. Production replacement administrator setup remains release work. Membership is managed in Cognito, never through signup or frontend storage. Current membership is checked on protected news requests; removing the group or MFA revokes the browser proof. Verify replacement access before removing the previous owner account's role.

Local API must use a LOCAL database with migrations through V19 applied and a matching Cognito issuer/audience. Never start a local process against production database credentials. Local sign-in uses the configured Cognito pool, so verify which pool is intended. Without the API, the admin displays an unavailable/locked state, not a demonstration of authorization.

## App management and Firebase plan

1. Start with read-only environment, app version, and release status. Keep DEV and production visibly distinct.
2. Connect Firebase server-side using environment-specific workload identity or a least-privilege secret from AWS Secrets Manager. Never put Firebase service account credentials in Angular, Git, or browser storage. A Firebase web client configuration is distinct from an administrative credential.
3. Model native device registrations per authenticated account, including platform, environment, locale, consent, invalid-token cleanup, and account deletion. Confirm iOS APNs/FCM configuration before delivery work.
4. Add notification drafts, audience counts, test-to-owner delivery, and a final audience/message review before sending. Enforce `notifications-admin`, request forgery protection, rate limits, idempotency keys, delivery status, and immutable send audit logs on the server.
5. Add content and configuration controls only as specific needs arise. Store changes with actor, timestamp, environment, and rollback history. Avoid unrestricted SQL, AWS consoles, arbitrary commands, and broad user data exports in the panel.

Do not create Firebase projects, send pushes, change production configuration, add app-admin members, or invoke release pipelines as part of building Newsroom. These require separate work and authorization.

## Deployment gate

Review the branch and tests, verify role assignment and MFA, apply migration through the normal release process, exercise authorized and unauthorized access against DEV, then separately approve production deployment. MAIN and its pipelines remain untouched during local implementation.

## Current local preview

Web runs on 127.0.0.1:4200 and API on 127.0.0.1:8080. Persistent local preview data uses sanctuary-news-cms-local, volume sanctuary-news-cms-data, and PostgreSQL port 15440. Schema is V19. The two existing articles were preserved through a private manual dump/restore. The local key in .env.news.local and backups are Git-ignored.

Integration tests use only the isolated news_cms_test database on 127.0.0.1:15439 and separate test schemas. Never point those tests at an application database. See infra/news-admin/compose.local.yml for the persistent local database configuration.

## Administrator MFA update

See [MFA and launch checklist](mfa-and-launch-checklist.md) for the current separate-account rollout. Newsroom now requires both news-admin membership and a database-backed proof of a completed Cognito TOTP challenge. A normal Sanctuary session alone no longer grants administrator access. DEV Outlook administrator enrollment and owner access are verified. Production MFA and identity provisioning are pending release. The local schema is now V19, with encrypted shared challenges and live membership/MFA checks. See [production security runbook](production-security-runbook.md) for current code, cloud prerequisites, and acceptance evidence.

## Explicit homepage placement and recoverable deletion

V18 adds placement (LEAD, SIDEBAR_1, SIDEBAR_2, MORE) and deleted_at. The editor shows a layout diagram and a named position selector. Placement governs Latest: main feature on the left, two supporting stories on the right, and More stories below. New articles default to More stories; there is no automatic promotion into a featured slot. Category tabs filter and reflow articles independently.

Saving a draft preserves its planned placement without displacing a live article. Publishing into a featured position moves the previous published occupant to More stories and audits that change. Featured slot replacement is transactional and versioned. Scheduled articles start in More stories; after they go live the editor can feature them. Existing lead-story data is migrated to LEAD without changing article text.

DELETE /admin/news/{id}?version=... moves an article to Trash, immediately removes it and its exclusively referenced images from public access, resets placement, and audits the action. It does not physically delete content, corrections, or images. Restore uses POST /admin/news/{id}/restore?version=... and returns the article as a private Draft in More stories; it never automatically republishes. The list has an explicit Trash view. Both operations require the same MFA-verified administrator session and mutation header as publishing. Optimistic version checks reject stale trash/restore actions. No hard-delete feature exists.
