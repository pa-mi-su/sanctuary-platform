# Announcements inbox and Settings

Current behavior for release 1.0.38 separates three things:

- **Announcements** are public English messages published by the owner. Anyone can read them in the app or website, even without an account or notification permission.
- **Phone alerts** optionally draw attention to an announcement. They require the existing device opt-in and OS permission. Turning them off does not hide the inbox.
- **Prayer/novena reminders** remain personal, device-scheduled notifications created from individual content pages. They are managed from Me and still require a signed-in account.

## Navigation

Native Me has a large bell at the top left and gear at the top right. The bell displays a red unread dot and opens Announcements; the gear opens Settings. Both destinations slide in as full pages with back navigation. Native Home has no hamburger. About Sanctuary lives in Settings, not on Me. Web keeps its existing navigation. Existing primary tabs stay unchanged. Me contains saved content, active/completed novenas, and native scheduled reminders. Settings contains language, announcement preferences, OS notification settings, and authenticated account actions.

The inbox shows the full message without an expander (messages are limited to 500 characters). A content action appears only for an associated prayer, saint, or novena; plain app announcements have no content button. Opening the inbox marks the loaded messages as read, with clearing one/Undo and clearing all/confirmation retained. This is inbox state, not proof that a message was read. An old alert can still open its specific message even if it was cleared from the inbox, provided the administrator has not withdrawn it. This does not restore the cleared entry to the regular inbox.

Guest receipts are local to the installation/browser and environment. On sign-in, guest reads/clears merge into account receipts; signed-in receipts synchronize through the API. Local clears made while signed in remain effective after logout/account deletion without exposing another account’s private receipt state. Fresh installations establish a publication cutoff; fresh accounts use their immutable server creation time so neither is flooded with old messages. Clearing a message affects only that reader. Clear all uses a publication cutoff so future messages still appear. The unread dot reflects the currently loaded feed page; older announcements remain available through Load more.

## Admin workflow

The Announcements composer has two modes:

1. **Announcement:** publishes to the public inbox, with **Also notify phones** as an optional switch.
2. **Test phone alert:** sends only to the owner's selected device and never publishes to the inbox.

Owner test alerts never create public inbox entries, even if the publication flag is submitted. Publication without a phone alert works without registered devices or enabled Firebase. Phone audience filters apply only to alerts; published messages are visible to everyone. Message limits remain 100 characters for the title and 500 for the body.

A single paginated **Announcements & alerts** list shows each message once, with separate **App inbox** (Visible in app / Removed from app / Not published) and **Phone alert** statuses (Not requested / Queued / Submitted / sending issues). Owner tests are labeled separately. **Remove from app inbox** removes a publication while keeping its historical record. Withdrawal hides the public message and skips remaining queued deliveries; alerts already accepted by Firebase cannot be recalled. Clearing old history preserves both active publications and pending sends. Active publications whose send history was cleared by an older admin version are included in the unified list, and removal restores their historical record until explicitly cleared. Successful submission still resets the composer and prevents duplicate resubmission with the same request ID.

## API and data

Flyway V26 adds `announcements`, per-account `announcement_receipts`, and `announcement_inbox_preferences`. Existing send history is deliberately **not backfilled** into the public inbox, avoiding exposure of historical tests.

- `GET /announcements?page=0`: public, 50 entries/page.
- `GET /announcements/{id}`: public full message; withdrawn entries return 404.
- `GET /me/announcements`: account receipt state.
- `POST /me/announcements`: read, clear, restore, or clearAll; identity comes from the authenticated JWT.
- Owner-authenticated `/admin-api/messages` retains existing preview/send/history behavior and adds publication flags, published list, and withdrawal.

Read/clear receipts cannot target another user. Consumer and owner authentication remain separate. Guest storage and late responses cannot replace a different account's receipt state. Existing send idempotency, snapshot checks, rate limits, and urgent Android/APNs alert payloads are preserved.

## Rollout

Deploy API/migrations and web first, then publish native 1.0.38 builds through the existing DEV-to-production process. Existing mobile auth/content/favorites/progress contracts remain supported while store approval is pending. A published alert opens `/announcements/{id}` in 1.0.26+; older/unknown native versions receive the original content link during rollout. Announcements remain plain text and destinations are restricted to Sanctuary content.

Android includes DEV announcement deep links only in its DEV manifest. iOS handles DEV push links explicitly; external DEV universal links still depend on existing associated-domain provisioning and can fall back to the website. Production AASA includes announcement paths.

## Consent and account lifecycle

The installation's phone-alert choice is separate from account state and OS permission. Login, logout, account deletion and app updates preserve it. New/unknown installs and OFF upgrades receive an invitation; restored ON with missing system permission invokes the native permission flow. System denial does not silently turn the app choice OFF. See [permission recovery verification](../notifications/restored-permission-recovery.md).

Account deletion alone does not clear general publications that were still visible to that installation. A message explicitly cleared before logout/deletion should remain cleared locally. System notifications already delivered to phones are a separate OS surface.

## Historical verification

- API: 99 tests, 98 passed and one existing database audit skipped; real isolated local PostgreSQL, migration V26, receipt isolation, publish-only, owner-test privacy, history/withdrawal separation, and old-app routing.
- Web: 55 tests passed, including guest/member isolation, failed requests, late responses, clear-all cutoff, cleared-message deep links, and independent publication management; development build passed.
- Android: DEV APK and 18 unit tests passed; UAT and production Kotlin compilation passed. Emulator checked full message, read/unread, clear/Undo, and clear-all confirmation against the isolated local API.
- iOS: DEV simulator build passed; simulator checked full message, read/unread, clear/Undo, clear-all confirmation, Menu, Settings, and guest Me separation.
- Visual previews use an explicitly labeled isolated local QA announcement, not real user content. No live announcements or phone alerts were sent for these checks.

Physical-device alert handoff and signed-in native cross-device receipt synchronization still need a DEV smoke test after deployment; backend receipt isolation is integration-tested locally.

## 1.0.27 follow-up: review stability and presentation

The previous recipient fingerprint included `push_devices.updated_at` and the token. A routine device registration refresh between review and send could therefore reject an unchanged audience. The fingerprint now uses a sorted canonical set of installation ID/account ID pairs. Last-sync updates and token rotation do not invalidate review; actual eligibility, recipient membership, and account changes still do. The dispatcher still checks opt-in and account identity at dispatch time. Duplicate-send protections remain unchanged. A genuine audience conflict clears the stale admin review while preserving the draft and send identifier, so another explicit review is required.

Regression verification: API 100 passed / one existing audit skipped, Android 19 tests and all flavor checks passed, web 58 tests and production build passed, iOS DEV simulator build passed. UI previews in `refinement-previews` use only explicitly local QA messages; no live messages were sent.

Native Me now has prominent bell and gear controls. Announcement content stays left-aligned for reading within centered, width-limited containers, with consistent heading, card-border, surface, and action styles across native platforms.

## Unified admin list follow-up

The visible-publication and send-history panels are combined into one aligned card list. Visibility and phone alerts remain independent fields; there is no duplicate message rendering or ambiguous “Withdraw” label. Removing an inbox publication requires confirmation that already-sent phone alerts cannot be recalled. Its card then reads “Removed from app”. One pagination control covers the complete list.

Checks: API 101 passed / one existing audit skipped; web 59 tests and production build passed. The UI rendering test checks visible, removed, publish-only, and owner-test distinctions. Localhost visual verification used the actual Angular component with synthetic read-only records; no admin sign-in or live records were used. `admin-unified-preview.jpg` shows the verified layout.

## Dismissible admin feedback

Status notices, history-load errors, and dialog errors have explicit dismiss controls; existing submission and global admin confirmations remain dismissible. Closing feedback changes only the message state, preserving the draft, reviewed audience, and open dialog. Close targets are at least 44 px.

Verified with 60 web tests, a production build, and a read-only synthetic localhost preview (`dismissible-admin-notices.jpg`). The DEV public feed still returned the existing announcement during the audit. Admin “Visible in app” indicates publication, while personal Clear/Clear all state can hide that same record for an individual and persists across app upgrades. No announcement was published, removed, or reset during this audit.

## App-icon badge lifecycle — 1.0.28

The app icon indicates an unacknowledged delivered phone alert, independently of the in-app bell's unread inbox state. APNs alerts include a binary badge of 1; iOS requests badge authorization with sound/alerts, including a one-time request for existing announcement opt-ins, and respects subsequent OS badge preferences. Foreground presentation includes badge support.

Opening a successfully loaded Announcements inbox acknowledges the announcement alerts currently delivered to that device. Clear/Clear all also acknowledges them after the inbox update succeeds. iOS removes delivered remote alerts and resets the icon badge; Android removes active notifications only from the sanctuary-announcements channel so the launcher can clear its dot. Pending and delivered local prayer/novena reminders remain separate. iOS also clears a stale badge when becoming active if no delivered remote alerts remain. Undo restores an inbox entry without re-posting a phone notification. This is device-local acknowledgement, not proof of delivery or reading.

Validation: iOS DEV simulator build passed. An isolated simulator received a synthetic APNs alert against a localhost-only feed: the app icon exposed “1 new item” before opening the inbox, and no badge after opening/clearing it. Screenshots: ios-icon-badge-before.jpg and ios-icon-badge-after.jpg. QA announcement consent was restored to off and no live alert was sent. Android DEV APK, 20 tests, and UAT/production Kotlin compilation passed; its channel-selection test excludes reminder and unknown channels. Both Firebase gateway tests passed, checking the actual serialized badge, sound, priority and destination payload. Physical Android launcher rendering remains controlled by the launcher and OS notification settings.
