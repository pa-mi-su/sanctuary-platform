# Notification choice across account lifecycle

Root cause: successful account deletion called iOS `resetConsent()` or Android `forget()`. Both wrote OFF and removed the Firebase token. The server also cascaded deletion of account-owned device registrations.

Changes:
- Remove those destructive mobile consent-reset helpers. Deletion clears account/session/reminder data and syncs the installation anonymously, just as logout does. Explicit notification settings and onboarding remain the only writers of announcement consent.
- Before deleting a user, transactionally detach their push installations. Preserve each installation ID, token, verification secret, revision, enabled/valid flags and last-sync time. This also preserves other phones on the same account without requiring them to be online.
- Skip pending deliveries targeting the deleted account before foreign keys are cleared. Accepted/history records remain intact; detaching must not reclassify queued member sends as guest sends.
- Login and account switching continue to attach the installation to the active account without changing the device preference. Account reminders still stop on logout/deletion.
- Version metadata: 1.0.36.

Validation:
- Full API suite: 113 tests, zero failures/errors, one unrelated skip, with a disposable localhost PostgreSQL instance. New integration tests exercise deletion with opted-in iOS and opted-out Android, untouched other-owner installation, credential-preserving anonymous resync, queued-send cancellation, and guest → account A → logout → account A → account B → logout for both platforms and both choices.
- Android: 34 unit tests and DEV debug build passed.
- iOS: DEV simulator build and compiled consent persistence/recovery/explicit opt-out regression passed.
- Actual native apps checked in isolated simulators against a synthetic localhost account fixture: login then account deletion retains ON on both platforms; explicitly setting OFF then logging in and out retains OFF on both platforms. The fixture simulates successful account deletion without deleting any real account. Real database deletion semantics are covered separately by the API integration test.
- No real account deletion or phone-alert sends were performed. OS notification permission behavior was not changed.

Deployment: install 1.0.36 after merging/deploying to DEV. This prevents future resets; it does not silently turn ON an existing OFF preference, which could be a genuine opt-out. No AWS database settings, backups, schema migrations or CI runner changes.
