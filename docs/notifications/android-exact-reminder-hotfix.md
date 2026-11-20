# Android selected-time reminder hotfix — 1.0.39

## Confirmed defect

Android 1.0.38 used `setAndAllowWhileIdle` when `canScheduleExactAlarms()` was false, including after a `SecurityException` while scheduling an exact alarm. This is an inexact alarm and can be deferred during idle. The detail UI allowed continuing without special access; reminders synced from iOS also bypassed the detail setup flow. A successful short foreground test therefore did not establish idle delivery reliability.

The owner reported 15:00 arriving at 15:47 and confirmed Alarms & reminders was not allowed. The released implementation confirms this unsafe fallback, but no physical-phone trace was captured for that incident. The exact 47-minute delay is not independently proven. Signing into both devices does not coordinate or suppress native delivery; each device schedules its own local notifications.

## Change

- Require exact-alarm access for new saves and edits, including edits from Me.
- Never install an inexact fallback. Cancel the pending operation if access is missing or revoked during scheduling.
- Keep synced reminder definitions and their selected hour/minute. Mark them as needing Android setup and show a recovery prompt; resume scheduling after permission is granted.
- Recheck old alarm delivery when access is missing, so an old inexact alarm cannot silently deliver late after upgrade.
- Preserve boot, app-update, time-zone and permission-grant recovery. Quiet reads preserve unchanged valid alarms.
- Sequence the announcement onboarding and reminder setup prompts to avoid stacking them.
- Explain setup in English, Spanish and Polish.

Android version becomes 1.0.39. iOS, API, web and database settings/backups are unchanged. This is an Android client fix: existing installed clients need the update and the user must allow Android's special access. The app cannot grant that permission itself. Exact-alarm access improves timing but cannot override force-stop, a powered-off phone, system notification permissions or every manufacturer's restrictions.

## Verification

- Android: 43 unit tests, zero failures; DEV app/instrumentation builds and production Kotlin compilation passed.
- Disposable Android emulator: denied access retains the selected time and installs no reminder alarm; granted access installs a native alarm with `window=0`; quiet reconciliation retains it; removal clears the definition.
- Native forced-idle delivery check passed: both the prayer and novena posted within ten seconds of the same target minute. The instrumentation test verified deep-idle state and restored battery/idle state afterward.
- iOS: native `UNCalendarNotificationTrigger` clock checks passed for API round trips, twelve time/zone cases, minute preservation and daylight-saving gap handling. Consent restoration, inbox/account transitions, and immediate progress/rollback regression checks also passed. iOS already uses repeating calendar triggers and has no Android-style inexact-alarm fallback.

No physical-phone/Apple Watch end-to-end test is claimed. The owner's iOS reminder arrived on time; Focus, sound settings and Apple Watch routing remain OS-controlled.
