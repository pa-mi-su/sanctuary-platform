# Inbox logout persistence and iPhone controls — 2026-11-18

## Cause and fix
Guest receipts were claimed during sign-in and removed from guest storage. Signed-in clear operations updated the account's server state only. Signing out therefore loaded an empty guest state and showed previously cleared public announcements again.

Both native clients now retain the last confirmed member receipt state as a local display snapshot for the signed-out view. This snapshot is separate from genuine guest activity uploaded at sign-in, so one account's read/clear history is not uploaded into another account. Guest undo updates both local display and guest activity. A local revision prevents a delayed background read from replacing a more recent clear snapshot. Existing server receipt APIs and database records are unchanged.

On iOS, clear-all uses a centered alert with explicit Cancel and destructive Clear all actions. Shared auth text/password controls use the same 52-point minimum height without extra vertical padding around the 44-point eye/clear controls. Login/register/reset/confirmation logic and credential handling are unchanged. A Rectangle wrapping the existing primary gradient disambiguates SwiftUI's View/ShapeStyle opacity overload in the updated Xcode compiler; appearance is preserved.

## Validation
- Swift receipt regression executable: guest transfer, member clear-all then logout/recreation, individual clear/undo, newer announcements, account isolation and delayed-read rejection.
- Android unit tests cover equivalent receipt-cache scenarios, alongside existing inbox filtering and consent tests.
- Android DEV APK and UAT/production Kotlin compilation.
- iOS simulator build with the updated local Xcode.
- Native QA used a localhost-only synthetic fixture. Logged into the fixture on both platforms, cleared the signed-in inbox, confirmed the member state was empty on both clients, logged out on both, then reopened each guest inbox and confirmed it remained empty.
- Visually checked the centered iPhone clear-all alert and equal-height empty email/password controls. Screenshots saved in /tmp/inbox-ios-clear-confirmation.jpg, /tmp/inbox-ios-field-heights.jpg, /tmp/inbox-ios-logout-cleared.jpg and /tmp/inbox-android-logout-cleared.jpg.
- Workflow isolation and git diff whitespace checks.

No real accounts, cloud database settings/backups, phone permissions or push delivery configuration were changed. No phone alerts were sent. Version metadata is aligned at 1.0.34. CI runner/cache/architecture configuration is unchanged; the receipt regression check is added to the existing iOS validation step.
