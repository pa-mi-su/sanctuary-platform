# Firebase messaging in Sanctuary

Branch `codex/admin-firebase-messaging`, based on main at `d2f62aa`. iOS marketing version, Android versionName, and web package version are **1.0.16**. Existing CI computes mobile build numbers independently.

## Owner workflow

Open Admin → Messages. Write one English title and message. Choose Home for an app announcement, or select an existing prayer, saint/feast day, or novena invitation. Existing content is searched and paginated; destinations are constructed by the server from the database slug, never arbitrary external links.

The default audience is **Send test to my device**. A test device must belong to the verified owner in the current environment's public Cognito directory and have announcements enabled. Install 1.0.16, sign in, opt in under Me, and refresh Users (to sync the directory) and Messages. Other available audiences are all eligible devices, iOS, and Android. Counts are devices, not unique people.

Review the preview, destination and recipient count, then check the explicit confirmation before sending. Editing invalidates review. Unsent drafts trigger the existing discard prompt when leaving admin sections. Drafts are not persisted across reloads.

History records the owner subject, title, message, destination, audience, creation time, and per-message recipient totals: pending, accepted, failed, skipped, uncertain. Refresh results manually. Firebase acceptance does not prove delivery or display. Open/read tracking and campaign scheduling are not implemented.

## Delivery and consent

FCM uses project `sanctuary-7f082`. Existing DEV/UAT/production iOS/Android app registrations and Apple push configuration are reused. No Firebase Authentication, Firestore, or Analytics implementation is added; account authentication remains Cognito. Public mobile SDK configuration is in the app resources. Private service-account keys stay in AWS Secrets Manager, never Git or Angular.

Only signed-in mobile installations register for this first version. Announcement consent defaults off and is separate from local prayer reminders. The UI labels/privacy copy are translated, but message content is English. Device registration records an installation UUID, owner account, FCM token, platform, app version, consent, token validity and last synchronization. Registration updates after login, foregrounding, token changes and preference changes. Tokens are not returned in admin responses or send history.

Account deletion cascades device records. Logout clears local consent, attempts server revocation, and invalidates the Firebase token. Network failure can delay revocation/token invalidation; operating-system notification settings remain the immediate device-level control. A different account on the same installation must opt in again. Queued deliveries retain the original recipient account ID; transferring an installation cannot redirect an earlier queued notification to the new account.

Audience selection excludes invalid, opted-out or unsynchronized-for-60-days devices. The recipient snapshot is checked again when submitting, and consent/account ownership are checked again before dispatch. Each message has a client-generated idempotency UUID, retained on network retries. The database outbox claims deliveries once across API instances. Interrupted/ambiguous sends become uncertain after ten minutes and are not automatically replayed; no exactly-once delivery guarantee is claimed. FCM handles final transport. Notification expiry is one day. Sending is limited to twenty messages per hour per environment.

## Deployment and activation

1. Deploy API first; Flyway applies **V23__push_messaging.sql**. This adds new tables and does not replace content or existing progress.
2. Deploy web and publish the new mobile builds through the normal DEV → production process. No native build is required for subsequent messages; this initial integration does require native releases.
3. Existing GitHub variables are reused: `API_DEV_FIREBASE_NOTIFICATIONS_ENABLED` / `API_PROD_FIREBASE_NOTIFICATIONS_ENABLED` and their `...FIREBASE_SERVICE_ACCOUNT_SECRET_ARN` values. Workflows inject `SANCTUARY_PUSH_ENABLED` and the private secret only when enabled. Default without configuration is false. The existing environment variables were already true during the audit; merging/deploying this branch therefore enables its sender in those environments.
4. ECS execution roles require read-only `secretsmanager:GetSecretValue` on their exact existing Firebase secret. `scripts/admin/firebase-secret-access.py dev` (or prod) prints the proposed policy; `--apply` attaches it and simulates permission. No secret values are read by that helper. **This IAM change was initially rejected by automatic approval review and remains pending explicit owner approval unless a later rollout record states otherwise.** Do not claim sending is activated until this permission is verified.
5. Test with the owner's physical DEV phone first. Verify consent off → no eligible recipients, consent on → test device appears, foreground and background receipt, tapping each destination, logout, token refresh, and opt-out. Keep production broadcasts unused until this succeeds. This physical-device delivery test cannot be replaced by a simulator build or Google credential authentication.

The initial audit confirmed six app registrations, enabled FCM V1, and configured APNs development/production keys for all three Apple registrations. DEV and production Secrets Manager credentials match the existing active key and successfully authenticated against Google's OAuth token endpoint. This check did not send a message or test APNs delivery. Initial IAM simulation showed both API execution roles lacked secret-read permission. No Firebase configuration was changed, no key was generated, and no broadcast was sent during implementation.

## Verification

The local PostgreSQL tests exercise opt-in filtering, stale audience rejection, idempotent submission, queue claims, opt-out before dispatch, uncertain-send handling, owner-only verified test targeting, and installation account transfer. Existing auth/cookie tests also guard a Firebase transitive-dependency regression: XML auto-negotiation is excluded so the API continues returning JSON.

Web tests cover preview invalidation and stable request IDs on retries. Full web production build, iOS simulator build and Android DEV/UAT/production compilation run locally. Real device delivery remains a release gate. Final local results: 31 web tests passed; 79 API tests ran with zero failures/errors and one pre-existing skip; 11 Android unit tests passed. iOS DEV simulator build passed, Android DEV/UAT/production variants compiled, and platform configuration verification passed. Seven workflow checks passed with 98 assertions. Existing warning-level bundle, stylesheet, CommonJS, Gradle and Swift concurrency warnings are separate from successful builds. Apple signing/provisioning profiles must include the Push Notifications capability; the unsigned simulator build does not validate distribution signing or APNs delivery.

This feature intentionally changes native source and triggers native validation/release workflows when their normal branch conditions apply. Later web/API-only edits remain isolated from native pipelines. Workflow tests also verify private credentials are injected only as secrets and only when enabled.

![Local Messages preview; sending disabled and no real recipients](previews/messages-local-preview.jpg)
