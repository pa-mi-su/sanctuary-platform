# Account prayer and novena reminders

Prayer and novena reminder definitions belong to the signed-in account. Both native apps use `GET/PUT/DELETE /me/reminders` and `DELETE /me/reminders/{kind}/{slug}`. The server derives the account from the authenticated JWT; clients cannot supply a user ID. The additive V27 migration creates reminder records only. No database settings, backup policies, or AWS infrastructure are changed.

## Scheduling contract

- Each record stores content kind, stable slug, display title, hour and minute. The same account sees the same selected time on Android and iOS.
- Time means the device's local wall-clock time. A reminder set for 14:55 remains 14:55 after travel rather than converting a UTC instant. The operating system handles locale-specific 12/24-hour display.
- Both platforms use 08:00 for a new reminder. iOS keeps time-picker values independent of today's date, so a DST transition cannot silently rewrite the stored time.
- At spring-forward, a nonexistent time moves forward by the DST gap (02:30 becomes 03:30 for a one-hour gap); fall-back uses the first occurrence only. Android calculation matches the native Apple trigger behavior verified with `UNCalendarNotificationTrigger`.
- The account has a maximum of 50 reminders. Updates to an existing reminder remain allowed at the limit. An account row lock prevents concurrent creates from exceeding it.
- Both apps fetch and reconcile on sign-in and foreground/resume, and after save, edit, delete or Clear all. The reminder list updates quietly every 30 seconds while visible in the foreground; there is no manual Refresh control. Another phone must open/resume Sanctuary to receive changes; this is not instant background synchronization. An offline/closed phone can retain its previous local schedule until it successfully syncs.
- A failed sync is visible and does not pretend a remote mutation succeeded. Existing known schedules are kept during transient network failures while the cached session remains valid. iOS waits for session bootstrap before treating an absent account as sign-out and persists the verified server account ID with its Keychain session for offline restoration. Remote writes are idempotent by account/kind/slug.
- Native schedules are removed on logout/account switching, without deleting account definitions. They are restored when that account signs in again. In-flight old-account responses cannot install reminders for a new account.
- Old unowned device-only reminders are cancelled rather than guessed into an account. A dismissible migration notice explains that they need to be set again.

## Native delivery

Android requests notification permission when setting a reminder. Starting with Android 1.0.39, selected-time reminders require `SCHEDULE_EXACT_ALARM` special access. When granted, the app uses `setExactAndAllowWhileIdle`; otherwise reminders are paused, definitions stay saved, and a setup prompt explains the required access. New saves and edits cannot bypass setup, including reminders synced from iOS. There is no approximate fallback. It does not request restricted `USE_EXACT_ALARM`. Boot, package replacement, clock/timezone changes and permission grants restore schedules. Each fired alarm reschedules only itself, preserving other reminders due in the same minute.

iOS uses a repeating hour/minute `UNCalendarNotificationTrigger`, default sound and a prayer/novena deep link. Pending and delivered item notifications are reconciled independently from announcements. Foreground local reminders request banner, sound and Notification Center list presentation so they remain available like Android tray notifications. Watch-versus-iPhone presentation remains Apple's routing policy; the app cannot force both to alert simultaneously. Phone permission, Focus, silent settings and wearable routing still affect presentation.

## Validation

Database integration tests use a disposable local PostgreSQL database, also covered by the existing CI PostgreSQL service via `ADMIN_TEST_DB_URL`. They exercise two clients of one account, another isolated account, 14:55 and 15:00 round trips, edits, deletion, Clear all, invalid requests, concurrent limit enforcement and account deletion cascades. No production data is involved.

Android unit tests cover exact minute preservation, next-day rollover, travel, spring-forward and fall-back. Native simulator checks use synthetic accounts and a loopback-only API harness which rejects push registration; no QA announcements or fake device registrations are created in AWS/Firebase.

A real paired Apple Watch and physical-device battery/Doze behavior still require owner testing after the DEV builds are installed.

## DEV PR verification (November 16, 2026)

| Check | Result |
| --- | --- |
| iOS creates prayer at 15:00 → Android Me | Displays 3:00 PM; API stores hour 15, minute 0 |
| Android creates novena at 14:55 → iOS Me | Displays 14:55; API stores hour 14, minute 55 |
| Delete a prayer on iOS → reopen Android Me | Prayer disappears; novena remains |
| Clear all on Android | Account list becomes empty; remote deletion verified |
| Android notification permission denied | Definitions remain visible; permission warning appears |
| Android exact-alarm access granted | Both native alarms use `window=0`, permission-authorized exact scheduling |
| Two reminders at 15:44 on Android | Both post in the tray; both independently reschedule to the next day |
| Two reminders at 15:50 on iOS | Both appear in Notification Center, including when received in the foreground |
| Tap iOS prayer notification | Opens the correct prayer detail |
| Android logout | Zero remaining item alarms and zero app notifications |
| iOS logout | Reminder owner/cache removed; both delivered reminder alerts removed from Notification Center |

API: 106 tests, 0 failures/errors, 1 pre-existing skip (including local PostgreSQL integration). Android: 25 unit tests pass; DEV builds and UAT/production Kotlin compilation pass. iOS DEV simulator build and actual-model/native-calendar clock checks pass. Web production build and workflow isolation checks pass. No real phone/Watch test is claimed by these simulator results.

## Reminder UI refinement

The reminder card displays the scheduled items and removal actions without synchronization instructions. The editor uses concise daily-reminder wording in English, Spanish and Polish. Transient update failures retry automatically while the list is visible. Quiet reads do not disable removal controls or reschedule unchanged native alarms.

Android navigation places the gesture inset outside the rounded navigation surface. All five items receive equal weights, centered icons and consistent outer padding; system gesture space no longer creates an oversized gap inside the bar.

The refinement was checked on both native simulators with synthetic local API data: while both Me screens stayed open, changing a reminder to 07:45 and deleting another record appeared automatically on both lists within the polling interval, without any taps. Both lists displayed only reminder rows/removal actions, and Android's gesture area was visibly outside the evenly spaced tab surface.
