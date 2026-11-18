# Fresh-install inboxes and alert upgrade invitations

## Behavior
- New mobile installations see the existing Enable alerts / Not now invitation. Installations with a saved OFF choice are invited once per marketing version after upgrading. ON choices are never re-prompted. Answering in Settings also records the current version.
- Not now preserves OFF and suppresses further invitations for that release. No choice is silently enabled. Login/logout/account deletion still preserve the stored choice.
- If phone permission is denied, the invitation stays available and offers Phone notification settings rather than claiming alerts are enabled. Returning after allowing permission completes opt-in. iOS invitation content scrolls at small sheet heights.
- A fresh installation establishes an inbox floor from the latest publication returned by its first successful feed load, before showing any rows or unread indicator. This uses server publication timestamps rather than the phone clock. Historical pages remain hidden; newer publications appear normally. An empty first feed initializes once at zero so later messages are not accidentally suppressed.
- The floor is installation-local and display-only, persists across launches and sign-in changes, and is never uploaded as an account clear-all receipt. It therefore does not clear another phone's account history. Existing installations with saved guest/member inbox receipts retain their history. Explicitly followed announcement links remain openable.
- Web applies the same fresh-browser floor. Alert invitation UI is mobile only. Version metadata is 1.0.37.

## Validation
- Android: 38 unit tests and DEV debug build passed. Includes fresh/existing installation, empty first feed, post-baseline messages, account isolation and per-version invitation policy.
- iOS: compiled consent and inbox scripts passed; DEV simulator build passed.
- Web: 69 unit tests and production build/share-preview generation passed. Tests verify old-history suppression, future messages and that the device floor is not uploaded into account receipts.
- Native UI on isolated Android/iOS simulators with a localhost fixture: fresh-install invitation shown; after Not now, old fixture publications absent and bell unread indicator clear; one newly added publication appears by itself; same-version relaunch does not re-prompt.
- Simulated upgrade by seeding only disposable QA invitation-version markers to 1.0.36 while consent remains OFF: both apps show the prompt in 1.0.37. Denying OS notification permission changes the primary action to Phone notification settings; Not now remains available and dismisses successfully.

No real phone alerts were sent and no real accounts were used. No API behavior, database schema/settings/backups or CI runner configuration changes.
