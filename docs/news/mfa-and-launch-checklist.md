> **Owner decision — October 13, 2026:** Keep the existing database setup simple. Use owner-managed manual backups only. Do not enable automated backups, schedule snapshots, change database encryption, or move/replace an RDS database instance without a new explicit instruction. The two existing manual snapshots may remain. AWS verification: DEV and production RDS retention are both 0, no pending RDS changes, and no active AWS Backup plans in us-east-1. The earlier maintenance-window proposal is withdrawn; it is not a pending approval or release requirement under the current scope.

# MFA and news release checklist

## Implemented and verified locally

- Separate administrator sign-in at /admin/news with local QR enrollment and six-digit TOTP verification.
- DEV Outlook account confirmed/enabled, TOTP-enrolled, and assigned news-admin. Ordinary consumer accounts are not forced to enroll.
- Client-bound signed ID tokens plus database-backed MFA proofs; live Cognito group/account/MFA checks and revocation.
- AES-GCM encrypted shared short-lived challenges, retry/replay limits, shared account throttling, private cookies, and strict CORS/mutation headers.
- News draft/image privacy, publication/scheduling, explicit homepage placement, corrections, and trash/restore.
- Local persistent preview database and preserved articles. Manual backups remain the owner's chosen process.
- Environment-scoped roles/keys/header policies and release variables are prepared. They do not deploy themselves.

## Required after the DEV PR

1. Merge only after dedicated DEV API/web validation and the environment-control validation pass. Opening the PR does not deploy. Merging into DEV runs the DEV API/web deployments.
2. Exercise the actual DEV deployment with real Cognito sign-in: MFA, save/reopen draft, image upload, preview, publish, schedule, unpublish, placement, correction, trash/restore, logout, stale-edit conflicts, and unauthorized requests.
3. Verify existing native consumer login/refresh/profile behavior against DEV's shared API. Native source, versions, and pipelines remain unchanged; backend compatibility still needs deployed acceptance.
4. Confirm actual ECS role permission, secret injection, RDS verified TLS, cookie/CORS headers, and deployed role revocation. Apply only the intended environment's hosting policy.
5. Complete production Outlook administrator provisioning/enrollment and recovery; verify replacement access before removing the previous owner's role. The owner enters passwords/OTP directly in the browser.
6. Promote/deploy only with explicit release authorization. UAT currently shares production backend/accounts; it is not isolated testing. Do not dispatch MAIN or production workflows as part of the DEV PR.

## Publication follow-up

Permanent article routes/metadata/social previews/sitemap, indexing policy, operational monitoring, publisher/contact/editorial information, and verified real content remain publication work. Firebase, notifications, rich text, multi-editor review, and app management are separate features.

See [production security runbook](production-security-runbook.md) and [validation results](security-hardening-results-2026-10-13.md).
