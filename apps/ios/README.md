# Sanctuary iOS

`apps/ios` is the native iOS app for Sanctuary. It is a SwiftUI client that uses the shared Java backend for content, auth, profile state, favorites, and novena progress.

## Stack

- Swift
- SwiftUI
- Xcode project
- Keychain-backed session storage
- URLSession API client
- Dev, UAT, and Prod schemes
- TestFlight and App Store Connect pipeline

## Structure

```text
apps/ios/
├── Sanctuary.xcodeproj
├── Sanctuary/
│   ├── Core/
│   │   ├── Application/    # Environment, API client, session/progress stores
│   │   └── Domain/         # Entities and repository protocols
│   ├── Features/           # Home, auth, calendar, saints, novenas, prayers, Me
│   ├── Resources/          # Bundled legacy/source JSON resources
│   ├── UI/                 # Shared theme/layout/localization
│   └── SanctuaryApp.swift
└── README.md
```

## Schemes And Environments

The project exposes:

- `Sanctuary-Dev`
- `Sanctuary-UAT`
- `Sanctuary-Prod`

Environment detection is based on bundle identifier suffix in [`Sanctuary/Core/Application/PlatformConfiguration.swift`](Sanctuary/Core/Application/PlatformConfiguration.swift):

- `.dev` -> `dev`
- `.uat` -> `uat`
- otherwise -> `prod`

The API base URL defaults to the production ECS API URL. You can override it with:

```text
SANCTUARY_API_BASE_URL
```

iOS never talks directly to PostgreSQL or RDS. All data access goes through the Sanctuary API.

## Product Areas

The app includes:

- home
- account access
- liturgical calendar
- saints list/detail
- novenas list/detail
- prayers
- search
- Me/profile
- about/support/privacy
- novena reminder scheduling foundation

## API And Auth

The app creates its current environment through [`Sanctuary/Core/Application/AppEnvironment.swift`](Sanctuary/Core/Application/AppEnvironment.swift).

At runtime it uses:

- `SanctuaryAPIClient` for backend calls
- `APIContentRepository` for API-backed content
- `RemoteUserProgressRepository` for authenticated user state
- `AccountSessionStore` and `KeychainStore` for session persistence

Legacy JSON files remain in [`Sanctuary/Resources`](Sanctuary/Resources) as bundled source/product material, but platform-backed content should flow through the API.

## Local Build

Validate the production scheme for simulator:

```bash
xcodebuild -project apps/ios/Sanctuary.xcodeproj -scheme Sanctuary-Prod -configuration Debug -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO build
```

Validate Dev:

```bash
xcodebuild -project apps/ios/Sanctuary.xcodeproj -scheme Sanctuary-Dev -configuration Debug -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO build
```

Validate UAT:

```bash
xcodebuild -project apps/ios/Sanctuary.xcodeproj -scheme Sanctuary-UAT -configuration Debug -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO build
```

## Release And CI

The iOS workflow is [`../../.github/workflows/ios-pipeline.yml`](../../.github/workflows/ios-pipeline.yml).

Release model:

- PRs validate iOS builds
- feature/dev promotion uploads Dev TestFlight builds
- UAT promotion uploads UAT TestFlight builds
- production builds upload to App Store Connect from `main`
- final App Store release approval remains manual

Related docs:

- [`../../docs/architecture/ios-to-platform-migration-tracker.md`](../../docs/architecture/ios-to-platform-migration-tracker.md)
- [`../../docs/deployment/ios-app-store-verification-checklist.md`](../../docs/deployment/ios-app-store-verification-checklist.md)
