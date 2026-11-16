# Announcements inbox and Settings

Version 1.0.26 separates three things:

- **Announcements** are public English messages published by the owner. Anyone can read them in the app or website, even without an account or notification permission.
- **Phone alerts** optionally draw attention to an announcement. They require the existing device opt-in and OS permission. Turning them off does not hide the inbox.
- **Prayer/novena reminders** remain personal, device-scheduled notifications created from individual content pages. They are managed from Me and still require a signed-in account.

## Navigation

Home has a single hamburger Menu containing Announcements, Settings, and About/Help. Its unread dot makes new announcements discoverable without a duplicate top action. Native menus use matching bottom sheets with large bordered action cards. Existing primary tabs stay unchanged. Me contains saved content, active/completed novenas, and native scheduled reminders. Settings contains language, announcement preferences, OS notification settings/details, and authenticated account actions.

The inbox shows the full message without an expander (messages are limited to 500 characters). A content action appears only for an associated prayer, saint, or novena; plain app announcements have no content button. Opening the inbox marks the loaded messages as read, with clearing one/Undo and clearing all/confirmation retained. This is inbox state, not proof that a message was read. An old alert can still open its specific message even if it was cleared from the inbox, provided the administrator has not withdrawn it. This does not restore the cleared entry to the regular inbox.

Guest receipts are local to the installation/browser and environment. Signed-in receipts synchronize through the API and are separate from guest receipts. Clearing a message affects only that reader. Clear all uses a publication cutoff so future messages still appear. The unread dot reflects the currently loaded feed page; older announcements remain available through Load more.

## Admin workflow

The existing Messages area becomes an Announcements composer with independent controls:

1. **Publish in Announcements:** public inbox publication.
2. **Also send a phone alert:** optional push to the selected opted-in audience.

Owner test alerts never create public inbox entries, even if the publication flag is submitted. Publication without a phone alert works without registered devices or enabled Firebase. Phone audience filters apply only to alerts; published messages are visible to everyone. Message limits remain 100 characters for the title and 500 for the body.

A separate paginated **Published in the app inbox** section lets the owner withdraw a message independently of sending history. Withdrawal hides the public message and skips remaining queued deliveries; alerts already accepted by Firebase cannot be recalled. Clearing completed sending history does not withdraw public announcements. Successful submission still resets the composer and prevents duplicate resubmission with the same request ID.

## API and data

Flyway V26 adds `announcements`, per-account `announcement_receipts`, and `announcement_inbox_preferences`. Existing send history is deliberately **not backfilled** into the public inbox, avoiding exposure of historical tests.

- `GET /announcements?page=0`: public, 50 entries/page.
- `GET /announcements/{id}`: public full message; withdrawn entries return 404.
- `GET /me/announcements`: account receipt state.
- `POST /me/announcements`: read, clear, restore, or clearAll; identity comes from the authenticated JWT.
- Owner-authenticated `/admin-api/messages` retains existing preview/send/history behavior and adds publication flags, published list, and withdrawal.

Read/clear receipts cannot target another user. Consumer and owner authentication remain separate. Guest storage and late responses cannot replace a different account's receipt state. Existing send idempotency, snapshot checks, rate limits, and urgent Android/APNs alert payloads are preserved.

## Rollout

Deploy API/migration and web first, then publish native 1.0.26 builds through the existing DEV-to-production process. A published alert opens `/announcements/{id}` in 1.0.26+; older/unknown native versions receive the original content link during rollout. Announcements remain plain text and destinations are restricted to Sanctuary content.

Android includes DEV announcement deep links only in its DEV manifest. iOS handles DEV push links explicitly; external DEV universal links still depend on existing associated-domain provisioning and can fall back to the website. Production AASA includes announcement paths.

## Verification

- API: 99 tests, 98 passed and one existing database audit skipped; real isolated local PostgreSQL, migration V26, receipt isolation, publish-only, owner-test privacy, history/withdrawal separation, and old-app routing.
- Web: 55 tests passed, including guest/member isolation, failed requests, late responses, clear-all cutoff, cleared-message deep links, and independent publication management; development build passed.
- Android: DEV APK and 18 unit tests passed; UAT and production Kotlin compilation passed. Emulator checked full message, read/unread, clear/Undo, and clear-all confirmation against the isolated local API.
- iOS: DEV simulator build passed; simulator checked full message, read/unread, clear/Undo, clear-all confirmation, Menu, Settings, and guest Me separation.
- Visual previews use an explicitly labeled isolated local QA announcement, not real user content. No live announcements or phone alerts were sent for these checks.

Physical-device alert handoff and signed-in native cross-device receipt synchronization still need a DEV smoke test after deployment; backend receipt isolation is integration-tested locally.

## 1.0.27 follow-up: review stability and presentation

The previous recipient fingerprint included `push_devices.updated_at` and the token. A routine device registration refresh between review and send could therefore reject an unchanged audience. The fingerprint now uses a sorted canonical set of installation ID/account ID pairs. Last-sync updates and token rotation do not invalidate review; actual eligibility, recipient membership, and account changes still do. The dispatcher still checks opt-in and account identity at dispatch time. Duplicate-send protections remain unchanged. A genuine audience conflict clears the stale admin review while preserving the draft and send identifier, so another explicit review is required.

Regression verification: API 100 passed / one existing audit skipped, Android 19 tests and all flavor checks passed, web 58 tests and production build passed, iOS DEV simulator build passed. UI previews in `refinement-previews` use only explicitly local QA messages; no live messages were sent.

Menus and Me → Settings now share prominent navigation cards. Announcement content stays left-aligned for reading within centered, width-limited containers, with consistent heading, card-border, surface, and action styles across native platforms.
