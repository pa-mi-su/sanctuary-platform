# Notification onboarding and update persistence — 2026-11-17

## Audit evidence and limit
The paired physical iPhone had DEV 1.0.31 (202). Its preferences contained a push sync revision and guest inbox state, but no announcement choice, installation ID or registered flag. The owner confirmed the announcement switch was off. The registration method returns before requesting an FCM token in this state. The DEV database held an older iOS 1.0.30 registration and no 1.0.31 registrations on either platform. Firebase accepted the recent sends to stored registrations; that does not prove display on a physical phone.

Comparing the November 17 update with the preceding working revision found no change to core push registration code, bundle IDs or Firebase credentials. No historical device log or preference snapshot establishes why the earlier choice was missing. It is not proven that the update erased it. Android was not connected for physical-device diagnostics. No speculative historical root cause is claimed.

## Resulting behavior
- New/legacy installations with no saved choice see a matching bottom-sheet invitation on iOS and Android. The Enable action invokes the native permission request. Not now saves an explicit opt-out.
- Existing enabled and disabled choices migrate without another invitation. Phone permission alone never implies announcement consent.
- iOS mirrors consent and installation identity/revision into Keychain; Android mirrors the preference and identity/revision into a separate app preference store. Keychain writes update existing items in place instead of deleting before adding.
- Missing primary consent can recover from the secondary copy; losing all copies results in a new invitation, not silent subscription. Explicit opt-outs win over older recovery values.
- Automatic registration remains on launch/resume/token changes. Settings shows a brief connection/permission problem on failure, without manual registration controls. Diagnostic logs include exception type and HTTP status, never tokens, credentials or account identity.
- If a lost installation ID collides with an existing Firebase token, the backend reuses that device row only when the original installation credential also matches. Token-only or mismatched-credential claims are rejected. Internal device ID/history remain stable.
- Every ordinary admin message is an inbox announcement. One optional checkbox also notifies phones. A distinct test mode only sends to the chosen owner device and never publishes publicly. Mode changes invalidate review; successful submission clears the composer.

## Validation
- Actual in-place 1.0.32 → 1.0.33 installation on isolated iOS/Android QA devices preserved an enabled choice, without showing onboarding again.
- An explicit opt-out survived a replacement install on both platforms.
- Fresh QA app state showed the invitation on both; Enable opened the native permission prompt, Allow dismissed onboarding and returned to Home.
- iOS consent regression executable covers unknown state, legacy migration, relaunch/update, recovery, explicit opt-out and secure-storage failure.
- Android consent tests cover unknown, enabled/disabled updates, recovery and stale backup precedence.
- API integration tests cover credential-verified recovery and rejection of mismatched credentials, opt-out, ordinary publication and private tests.
- Native QA API endpoints were local-only. No real phone alert was sent, and physical APNs/FCM delivery still requires the merged DEV build plus a deliberate device test.

AWS database settings, backups, existing user records and notification registrations were not modified. No CI runner/cache/architecture change was made.
