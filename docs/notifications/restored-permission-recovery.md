# Restored announcement consent and system permission

Version: 1.0.38. Branch: codex/restored-consent-permission-recovery.

## Confirmed failure

An iOS installation restored `sanctuary.announcements=true`, but its system notification authorization was still `notDetermined`. Restored ON suppressed the app invitation, while push sync only requested permission after authorization already existed. It therefore never requested Apple permission or completed APNs registration. Inbox publications are fetched independently and could still appear.

Android had the equivalent invitation gate: restored ON skipped onboarding even when POST_NOTIFICATIONS was missing.

## Changes

- Recover missing system permission for restored ON. OFF does not request system permission automatically.
- Coalesce concurrent iOS permission requests; ignore stale permission results after an explicit choice changes.
- Keep app consent separate from system permission. Denial leaves saved ON intact and displays a permission warning.
- On iOS, request missing permission before opening Settings. Authorized users go to the app notification page; blocked users use Apple's app-settings URL, with an app-settings fallback if opening fails. Both entry points share this implementation.
- Android uses its existing app-specific notification Settings intent and resumes registration after permission is enabled.
- Surface APNs registration failure without logging tokens or credentials.

## Validation

- Reproduced the old iOS failure in an isolated iOS 26.1 simulator: restored ON and actual authorization `notDetermined`, without a permission dialog. Upgraded that same installation: native Apple permission dialog appeared automatically.
- Denied Apple permission: app switch remained ON and permission warning appeared. Enabled notifications in Settings: lock-screen, Notification Center, banners, sounds and badges were enabled; app retained ON and persisted successful push registration.
- First-use simulator caveat: both Apple-supported Settings URLs initially fell back to Settings home. Navigating Apps → Sanctuary showed the app's notification controls. After authorization, the in-app link opened Sanctuary's notification page directly. The first-use deep link still requires verification on a physical iPhone; this test is not recorded as passing.
- Android emulator: restored ON from backup with runtime permission missing; native dialog appeared. Denied, opened Sanctuary-specific notification Settings, enabled permission and returned. Invitation closed, ON remained set and push registration completed.
- Android: 39 unit tests, zero failures; DEV debug APK build passed.
- iOS executable regression checks passed: consent migration/recovery/opt-out/storage failure; permission unknown/allowed/blocked/OFF and concurrent requests; inbox account isolation; reminder clock/DST; optimistic progress/rollback.
- Web: 69 tests passed. Web notification behavior was not changed.
- DEV iOS simulator build passed.

## Remaining release verification

Install the new signed builds on physical iOS and Android devices. Check the first-use Settings link and send a new test alert while each phone is locked/backgrounded; confirm sound/banner/icon badge and content navigation. Simulator authorization/registration is not proof of APNs/FCM lock-screen delivery.

No database settings, backups, API contracts, or pipeline runner configuration were changed.
