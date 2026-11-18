# New-account announcement baseline

The public feed returns all active publications. A newly created account had no receipts, so clients rendered historical publications as unread.

The authenticated receipt endpoint now returns the later of the profile's immutable `created_at` and its explicit clear-all cutoff as `clearedBefore`. Android, iOS and web already filter list entries and unread indicators using this value. A normal login or refresh never advances it. Guest receipt transfers cannot lower it. Clearing newer messages can advance it normally.

This applies the same rule to existing profiles: announcements predating their profile creation are hidden; later publications retain their existing read/clear state. Signed-out public browsing and explicitly opened announcement links retain their existing behavior. No announcements are deleted and no phone alerts are sent by this change.

Validation:
- Full API suite against a disposable localhost PostgreSQL database: 111 tests, zero failures/errors, one unrelated skip. Includes old publication → new profile → later publication, repeated loads, guest transfer, clear-all and zero push deliveries.
- Android: 34 unit tests pass, including the actual inbox filter at/before/after the baseline.
- Web: 68 tests pass, including authenticated transition, empty old backlog, older pagination, stable refresh and unread future announcements.
- iOS: compiled domain/receipt regression script passes, using the same visibility predicate as the inbox view.

No AWS database settings, backups, migrations or deployed data were changed. No physical-device push-delivery test was performed; push delivery is outside this inbox visibility fix.
- iOS Sanctuary-Dev simulator build: passed.
- Web production build and 628 share-preview generation: passed; existing stylesheet-budget and qrcode CommonJS warnings remain.
