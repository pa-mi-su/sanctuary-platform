# Sanctuary iOS

`apps/ios` is Sanctuary's native SwiftUI client. It retrieves runtime content and account state from the shared Spring Boot API.

Current release identity:

- production bundle: `com.pamisu.Sanctuary`
- display name: `Sanctuary: Prayer and Peace`
- marketing version: `1.0.38`
- deployment target: iOS 16.6
- Swift language mode: 5.0

## Product and interaction model

`AppShellView` supplies five lazy-loaded tabs:

- Home
- Novenas
- Liturgical
- Saints
- Me/account access

Home opens daily readings, prayers, rosaries, intention search and patronage search. Me has a bell for Announcements and gear for Settings. Settings contains Account, General/language, Notifications, Support & Legal and About Sanctuary. Calendar screens support day/week/month views; detail screens support favorites, sharing, related navigation, and novena progress. The UI supports English, Spanish, and Polish.

Application state is divided by responsibility:

- `AppEnvironment`: creates platform configuration, URLSession API client, content repository, and search repository
- `AccountSessionStore`: auth flow, Keychain session, refresh, profile/preferences, and account deletion
- `UserProgressStore`: favorites, commitments, optimistic updates, and reminder synchronization
- `APIContentRepository`: maps API DTOs to domain entities
- `RemoteUserProgressRepository`: authenticated progress requests with one refresh/retry after session rejection
- feature view models: local list/search/date presentation

## Source layout

```text
Sanctuary/
├── Core/
│   ├── Application/  # environment, configuration, sessions, progress, links, reminders
│   ├── Data/API/     # URLSession client and API repositories
│   ├── Data/Local/   # local search/progress implementations used by supporting paths
│   └── Domain/       # entities and repository protocols
├── Features/
│   ├── About/ Auth/ Calendar/ Home/ Me/
│   ├── Novenas/ Parish/ Prayers/ Saints/ Search/ Shell/
├── Resources/        # bundled legacy/reference JSON
├── UI/               # theme, localization, responsive layout
└── SanctuaryApp.swift
```

Bundled JSON is not the production runtime source of truth. `AppEnvironment.current()` creates `APIContentRepository`; PostgreSQL-backed API responses drive content.

## Schemes and environments

| Scheme           | Bundle ID                  | Environment | API                                    |
| ---------------- | -------------------------- | ----------- | -------------------------------------- |
| `Sanctuary-Dev`  | `com.pamisu.Sanctuary.dev` | `dev`       | `https://dev-api.mydailysanctuary.com` |
| `Sanctuary-UAT`  | `com.pamisu.Sanctuary.uat` | `uat`       | `https://api.mydailysanctuary.com`     |
| `Sanctuary-Prod` | `com.pamisu.Sanctuary`     | `prod`      | `https://api.mydailysanctuary.com`     |

`PlatformEnvironment.current()` selects the environment by `.dev` or `.uat` bundle suffix; every other bundle is production. `SanctuaryAPIBaseURL` in generated Info.plist settings supplies the scheme URL. A valid `http`/`https` process environment variable `SANCTUARY_API_BASE_URL` takes precedence for local runs.

Authentication is enabled in every `PlatformConfiguration`. iOS does not access PostgreSQL/RDS directly.

## Native authentication and persistence

`SanctuaryAPIClient` uses the native endpoints `/auth/login`, `/auth/refresh`, and `/auth/reset-password`. `AccountSessionStore` stores access, ID, refresh tokens, expiration, email, and display name as one Codable session in Keychain service `com.pamisu.Sanctuary.session`.

For authenticated API requests, the ID token is preferred and the access token is the fallback. If `/me` state rejects a token, `RemoteUserProgressRepository` asks the session store to refresh once and retries. If refresh is unavailable or fails, the stored session is cleared.

Favorites and commitments are API-backed for authenticated users. `UserProgressStore` clears its in-memory state on sign-out and reloads it when the authenticated user changes.

## Universal links, sharing, and reminders

The app entitlement registers `applinks:mydailysanctuary.com`. `SharedContentLink` accepts both the apex and `www` hosts and parses:

- `/saints/{slug}`
- `/novenas/{slug}`
- `/prayers/{slug}`

Opening a link selects the appropriate tab and presents an API-backed full-screen detail. Shares emit the same canonical apex-domain URL.

`NovenaReminderScheduler` reconciles signed-in account definitions from `/me/reminders` into repeating `UNCalendarNotificationTrigger` requests. Users choose a daily hour/minute for each prayer/novena; Me supports editing, thumbnails/content navigation, deletion and Clear all. Sync runs on login/foreground and quietly while the list is visible. Logout cancels local account schedules; signing back in restores definitions. Fixed 08:00/20:00 legacy controls are not exposed in Me. See [the reminder contract](../../docs/notifications/account-reminders.md).

## Announcements and native phone alerts

`PushNotifications` configures environment-specific Firebase resources, APNs registration, private installation identity and saved consent. `AnnouncementConsent` backs up the installation choice in Keychain. Restored ON requests missing Apple permission before APNs/FCM registration; concurrent requests share one dialog. OFF is never automatically changed to ON. Denial preserves app consent and shows a Settings action. Logout/account deletion detach identity without opting the installation out.

The public inbox is separate from phone alerts. New installations/accounts use publication cutoffs; guest receipts transfer at login and local clears survive logout. Opening the inbox acknowledges remote phone alerts and clears their icon badge without deleting prayer/novena schedules. Apple/Watch routing and OS permission still control presentation. See [announcements](../../docs/announcements/README.md) and [permission recovery tests/limitations](../../docs/notifications/restored-permission-recovery.md).

## Local validation

Verify bundle-to-API mapping:

```bash
swiftc \
  apps/ios/Sanctuary/Core/Application/PlatformConfiguration.swift \
  apps/ios/Scripts/verify-platform-configuration.swift \
  -o /tmp/verify-sanctuary-platform
/tmp/verify-sanctuary-platform
```

Unsigned simulator builds from repository root:

```bash
xcodebuild -project apps/ios/Sanctuary.xcodeproj \
  -scheme Sanctuary-Dev -configuration Debug-Dev \
  -destination 'generic/platform=iOS Simulator' \
  CODE_SIGNING_ALLOWED=NO build

xcodebuild -project apps/ios/Sanctuary.xcodeproj \
  -scheme Sanctuary-UAT -configuration Debug-UAT \
  -destination 'generic/platform=iOS Simulator' \
  CODE_SIGNING_ALLOWED=NO build

xcodebuild -project apps/ios/Sanctuary.xcodeproj \
  -scheme Sanctuary-Prod -configuration Debug-Prod \
  -destination 'generic/platform=iOS Simulator' \
  CODE_SIGNING_ALLOWED=NO build
```

There is no XCTest target. CI runs executable checks for platform mapping, announcement consent/permission concurrency, inbox receipts, reminder clocks/DST and progress feedback, followed by a simulator build. Unsigned builds verify compilation; interactive Keychain/notification testing needs an appropriately signed build. Simulator permission tests do not prove physical APNs delivery.

## CI and App Store Connect

`.github/workflows/ios-pipeline.yml`:

| Trigger                                                    | Result                                                                                   |
| ---------------------------------------------------------- | ---------------------------------------------------------------------------------------- |
| PR to `dev`, `uat`, `prod`, or `main` with iOS app changes | map target branch to scheme, verify platform configuration, build unsigned simulator app |
| PR that changes only `ios-pipeline.yml`                    | verify platform configuration; app diff detector skips simulator compilation             |
| Push to `dev` with iOS code changes                        | validate, sign/archive `Sanctuary-Dev`, upload to TestFlight                             |
| Push to `uat` with iOS code changes                        | validate, sign/archive `Sanctuary-UAT`, upload to TestFlight                             |
| Push to `main` with iOS code changes                       | validate, sign/archive `Sanctuary-Prod`, upload to App Store Connect                     |
| Manual dispatch                                            | run selected `dev`, `uat`, or `prod` target                                              |

The workflow uses `macos-15-intel`, caches Swift packages and separate simulator/device compilation intermediates, and validates only the runner architecture for simulator builds. Device archives retain their normal architecture settings. See [cache details](../../docs/deployment/ios-build-caching.md).

Each upload queries App Store Connect for the latest build number of that bundle ID/version, increments it, archives with that `CURRENT_PROJECT_VERSION`, exports the IPA, and uploads with Apple's API key tooling. Signing certificates, provisioning profiles, API key, issuer, and team ID come from GitHub environment secrets/variables.

Uploading does not itself choose TestFlight testers, submit an App Store version for review, or release it publicly; those remain App Store Connect operations.

Markdown-only changes under `apps/ios` do not start the workflow. A workflow YAML change can validate on a PR but does not cause a native upload on push by itself.

## External/product integrations

- daily readings: USCCB web pages, localized to the Spanish URL pattern when appropriate
- parish finder: Core Location/MapKit-based nearby search
- support: `info@mydailysanctuary.com`
- privacy/support documents: native views under `Features/About`
- associated domain: `mydailysanctuary.com`

Operational release checklist: `../../docs/deployment/ios-app-store-verification-checklist.md`.
