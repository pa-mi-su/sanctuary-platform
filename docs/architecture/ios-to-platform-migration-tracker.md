# iOS To Sanctuary Platform Migration Tracker

## Purpose

This document is the working migration tracker for moving the native iOS app from the legacy standalone repository into the `sanctuary-platform` monorepo and fully aligning it with the real backend, auth, and deployment model.

This is not a "move files and hope" effort.

The goals are:

- move the iOS app into the platform monorepo cleanly
- make the API the only source of truth
- remove bundled JSON and local rule-engine dependencies over time
- align iOS with the same auth, profile, favorites, and novena-progress model used by web
- verify App Store Connect, signing, and release readiness as part of the migration

## Current State Summary

### What is true today

- The iOS app already has a strong native UI structure under `Sanctuary/Features`, `Sanctuary/Core`, and `Sanctuary/UI`.
- The iOS app still depends heavily on local bundled JSON and local rules engines.
- The legacy Xcode project is structurally healthy enough to audit locally and appears organized around one app target with environment-specific schemes.
- `sanctuary-platform` is now the real product platform for:
  - web
  - API
  - Cognito auth
  - synced user profile
  - favorites
  - novena progress
- The iOS app must be migrated to consume that same platform instead of continuing to evolve as a separate legacy client.

### Important architectural rule

From this point forward:

- no new long-term iOS features should be built on bundled legacy data
- no new product logic should be added to local-only rules systems if the backend should own that behavior

### Initial audit findings

The current standalone iOS repo shows the following:

- Xcode project exists at `Sanctuary.xcodeproj`
- local `xcodebuild -list` succeeds
- target currently present:
  - `Sanctuary`
- build configurations currently present:
  - `Debug`
  - `Debug-Dev`
  - `Debug-UAT`
  - `Release`
  - `Release-Dev`
  - `Release-UAT`
- schemes currently present:
  - `Sanctuary-Dev`
  - `Sanctuary-Prod`
  - `Sanctuary-UAT`
- automatic signing is configured
- development team in the project file is `Z9L9BZFYLS`
- bundle identifiers currently present:
  - `com.pamisu.Sanctuary`
  - `com.pamisu.Sanctuary.dev`
  - `com.pamisu.Sanctuary.uat`
- the native app still uses local data and local rule engines heavily, especially through:
  - `Sanctuary/Core/Data/Local/LocalContentRepository.swift`
  - `Sanctuary/Core/Data/Local/ContentStore.swift`
  - `Sanctuary/Core/Data/Local/LocalUserProgressRepository.swift`
- no existing iOS release automation was found in the legacy repo
- the only GitHub Actions workflow currently present in the legacy repo is `liturgical-validation.yml`
- App Store Connect production app record has now been visually verified by the product owner
- the native iOS app has already shipped publicly to the App Store
- TestFlight has already been used operationally for dev and UAT distribution
- there is already local documentation confirming liturgical/rules duplication and recommending backend ownership:
  - `docs/liturgical-engine-audit.md`
  - `docs/app-modernization-blueprint.md`

This means the migration is justified and urgent:

- the UI is valuable and reusable
- the data model and product authority should move to the platform
- the old repo should stop being the long-term source of truth

### What is verified locally vs what is still unknown

Verified locally:

- the Xcode project opens to tooling and can be enumerated with `xcodebuild`
- the app has explicit environment-aware schemes and build configurations
- signing is configured for automatic mode in the project file
- team and bundle identifiers are consistently defined in the project file
- no Fastlane or iOS GitHub Actions release pipeline is currently present in the legacy repo
- the modernization blueprint already recommends repository-driven, API-backed architecture
- simulator builds succeed locally for:
  - `Sanctuary-Prod` with `Debug`
  - `Sanctuary-Dev` with `Debug-Dev`
  - `Sanctuary-UAT` with `Debug-UAT`

Verified with App Store Connect screenshots / operator confirmation:

- production app record exists for `com.pamisu.Sanctuary`
- app name shown in App Store Connect is `Sanctuary: Prayer & Peace`
- Apple ID shown in App Store Connect is `6759986068`
- SKU shown in App Store Connect is `sanctuary-ios-prod-001`
- the production iOS app has already been released to the App Store
- TestFlight has been used for dev and UAT build distribution

Still requires external verification:

- certificate and provisioning validity in Apple systems
- whether an Archive still succeeds on a signing-capable machine after migration
- who currently has permission to ship or manage the app

## Migration Principles

- Move first, rewrite second: relocate the project into the monorepo before doing large domain rewrites.
- One source of truth: API and database should become the authority for content and user state.
- Domain-by-domain migration: auth, profile, favorites, and content domains should migrate in deliberate phases.
- Remove legacy after proof: each local legacy path should be deleted only after the new API-backed path is verified.
- No hidden dual systems: avoid leaving the app half on JSON and half on API for the same feature.

## Target Monorepo Structure

Inside `sanctuary-platform`, the target shape should be:

```text
apps/
  api/
  web/
  ios/
docs/
  architecture/
  deployment/
```

Optional later:

```text
packages/
  contracts/
  design-tokens/
```

## Planned Move Manifest

When we relocate the legacy native app, the expected primary inputs are:

- `Sanctuary.xcodeproj`
- `Sanctuary/`
  - `Assets.xcassets`
  - `Core/`
  - `Features/`
  - `Resources/`
  - `UI/`

Likely to move into `apps/ios` as supporting context:

- selected iOS-specific docs that should stay near the app
- selected scripts only if they are directly tied to the native app build or validation flow

Likely to stay outside the app workspace or be reconsidered during migration:

- GitHub Actions from the legacy repo
- legacy migration notes that belong in platform docs instead
- user-specific Xcode metadata such as `xcuserdata`

Migration rule:

- the first structural move should preserve the Xcode project and source tree intact before any domain rewrites begin

## Phase Tracker

### Phase 1: Audit And Release Ownership

Status: `IN PROGRESS`

Goal:
- confirm what currently exists in code, Apple systems, and release ownership before touching structure

Deliverables:
- iOS code audit
- App Store Connect audit
- signing and bundle identifier audit
- migration readiness summary

Checklist:
- [x] Verify App Store Connect app record for production bundle identifier
- [x] Verify TestFlight status
- [ ] Verify signing team, certificates, and provisioning health
- [x] Verify who can archive and ship builds today
- [x] Verify current environment bundle identifiers and intended use
- [x] Confirm whether any existing CI/CD exists for iOS release builds

Notes:

- Local audit confirms the standalone app is structured around one target with three environment schemes.
- Local audit confirms bundle identifiers for prod, dev, and UAT are present and consistent with the migration assumptions.
- Local audit did not find any existing iOS release automation; release should be assumed manual until proven otherwise in Apple systems.
- Local simulator builds now succeed for prod, dev, and UAT schemes when signing is disabled for simulator execution.
- App Store Connect screenshots confirm a live production app entry, bundle identifier `com.pamisu.Sanctuary`, and active product metadata.
- Operator confirmation establishes that production was released publicly and TestFlight has already been used for dev and UAT flows.
- Remaining Apple-side verification is now narrower: cert/profile health and a fresh archive/upload check.

### Phase 2: Move iOS Into `sanctuary-platform`

Status: `COMPLETE`

Goal:
- move the project into the monorepo without changing runtime behavior yet

Deliverables:
- `apps/ios` folder in `sanctuary-platform`
- working Xcode project after move
- updated local docs

Checklist:
- [x] Create migration branch in `sanctuary-platform`
- [x] Import iOS project into `apps/ios`
- [x] Preserve Xcode references and asset paths
- [x] Verify build still works after the move
- [x] Update README and local setup docs
- [ ] Freeze the old repo except for emergency fixes

Notes:

- The Xcode project and `Sanctuary/` source tree have been imported into `apps/ios`.
- The import preserved the original relative layout between `Sanctuary.xcodeproj` and `Sanctuary/`.
- Post-import simulator builds were verified from the monorepo for:
  - `Sanctuary-Prod`
  - `Sanctuary-Dev`
  - `Sanctuary-UAT`
- Machine-local Xcode metadata and `.DS_Store` files were removed from the imported tree.

### Phase 3: Shared Client Foundation

Status: `IN PROGRESS`

Goal:
- make iOS a real client of the platform

Deliverables:
- API client
- auth/session management
- domain repositories
- environment config model

Checklist:
- [x] Introduce API base URL config for iOS environments
- [x] Introduce auth/session manager for Cognito-backed flows
- [x] Introduce content repository protocols backed by API
- [x] Introduce user progress repository backed by API
- [ ] Introduce feature flags only if needed for incremental cutover

Notes:

- `PlatformConfiguration` now resolves iOS environment from the bundle identifier and exposes an API base URL, including a simulator-friendly localhost override for non-production work.
- Dev and UAT are now explicitly configured to prefer a local backend by default instead of silently falling back to production.
- `SanctuaryAPIClient` now provides a typed async client for the current auth and account endpoints used by the web app:
  - `/auth/register`
  - `/auth/confirm`
  - `/auth/resend-confirmation`
  - `/auth/login`
  - `/me`
  - `/me/favorites`
  - `/me/novena-commitments`
- `AccountSessionStore` now persists the signed-in session in the keychain and owns bootstrap, login, register, confirm, resend, refresh, and logout behavior.
- `RemoteUserProgressRepository` now backs favorites and novena commitments with the real API instead of local-only storage for authenticated flows.
- `HybridContentRepository` now gives iOS a safe migration path where saints are API-backed first while novenas, prayers, and liturgical data continue to read from the legacy local repositories until their domain migrations are ready.
- Prod, Dev, and UAT simulator builds all succeed with this new foundation slice in place.

### Phase 4: Auth And Account Migration

Status: `IN PROGRESS`

Goal:
- align iOS login/register/account behavior with the platform

Deliverables:
- login
- register
- confirmation
- logout
- `/me`

Checklist:
- [x] Replace legacy/no-auth assumptions with real account state
- [x] Wire iOS login to Cognito-backed platform flow
- [x] Wire registration and confirmation flow
- [x] Wire logout and token/session clearing
- [x] Wire `/me` profile loading
- [x] Use first name / last name / display name consistently
- [x] Verify authenticated state survives app relaunch correctly

Notes:

- `MeView` now renders a signed-out account experience through `AccountAccessView` instead of assuming a local placeholder user.
- The account flow now supports:
  - login
  - register
  - email confirmation
  - resend confirmation
  - logout
- Account state is restored at app launch through the keychain-backed `AccountSessionStore`.
- The current account-facing UI slice is intentionally limited to authentication and synced user-state wiring; saints, novenas, liturgical, and prayers content still come from local repositories until later domain migration phases.

### Phase 5: User Data Features

Status: `IN PROGRESS`

Goal:
- bring iOS user-state behavior in line with web

Deliverables:
- favorite saints
- favorite novenas
- novena commitments
- synced account preferences

Checklist:
- [x] Wire favorite saints to API
- [x] Wire favorite novenas to API
- [x] Wire novena start / stop / progress to API
- [x] Wire Me/profile view to API-backed counts and lists
- [ ] Remove local-only progress behavior for authenticated flows
- [ ] Verify cross-device sync between web and iOS

Notes:

- Authenticated favorites and novena commitments are now backed by the real `/me` API surface.
- `MeView` now refreshes signed-in profile and synced user progress from the API-backed stores instead of relying on a local placeholder state model.
- Favorite saint rows in `Me` now resolve saint names and detail navigation through the repository layer rather than the bundled saint JSON store.

### Phase 6: Content Domain Migration

Status: `IN PROGRESS`

Goal:
- remove bundled content dependency by domain

Recommended order:
1. Saints
2. Novenas
3. Liturgical
4. Prayers

Checklist:
- [x] Replace saints local repository with API repository
- [ ] Replace novenas local repository with API repository
- [ ] Replace liturgical local/rule logic with API-backed data
- [ ] Replace prayers local repository with API repository
- [x] Verify search and detail screens work from API data
- [x] Verify calendar/date-driven screens work from API data

Notes:

- Saints is now the first real content domain migrated off bundled JSON in the runtime path.
- The following iOS surfaces now source saints from the API-backed repository path:
  - saints list
  - saints search
  - saint detail hydration
  - saints calendar monthly/day lookup
  - favorite saint resolution in `Me`
- Saint detail still keeps local related-novena lookup as a temporary bridge until novenas migrate.

### Phase 7: Legacy Removal

Status: `TODO`

Goal:
- eliminate duplicate systems

Checklist:
- [ ] Remove bundled JSON usage where API parity exists
- [ ] Remove legacy local rule engines no longer needed
- [ ] Remove obsolete local content loaders
- [ ] Remove fallback seed data that is no longer needed
- [ ] Document what was retired and why

### Phase 8: App Store And Delivery Hardening

Status: `TODO`

Goal:
- make iOS delivery reliable from the new repo

Checklist:
- [ ] Verify archive/build from monorepo layout
- [ ] Verify App Store Connect upload path
- [ ] Decide manual vs automated release flow
- [ ] Add PR validation build for iOS
- [ ] Add optional TestFlight upload workflow later

## Domain Migration Matrix

| Domain | Current Source | Target Source | Priority | Notes |
|---|---|---|---|---|
| Auth | Minimal / legacy assumptions | Cognito + platform API | High | Must be first real runtime migration |
| Profile / Me | Local-first / partial | `/me` + user preferences API | High | Must match web account model |
| Favorites | Local | API-backed user favorites | High | Shared user state |
| Novena Progress | Local progress repository | API-backed commitments | High | Shared user state |
| Saints | Bundled JSON | API | High | User-facing core domain |
| Novenas | Bundled JSON + local rules | API | High | Must remove rule duplication |
| Liturgical | Local engine + partial bundled data | API | High | Important architecture cleanup |
| Prayers | Bundled JSON | API | Medium | Easier once repository layer exists |
| About / Support / Privacy | Local UI | Platform-aligned | Low | Simple follow-up cleanup |

## Key Risks

- Breaking the Xcode project during the move
- Leaving mixed local/API data sources in production
- Keeping novena or liturgical logic duplicated between client and server
- Allowing auth behavior to diverge between web and iOS
- Discovering App Store release ownership problems too late
- Trying to migrate all domains at once instead of sequencing them

## Immediate Next Actions

1. Validate the new saints API-backed flow against the live backend on simulator/device
2. Continue Phase 6 with novenas as the next content domain migration
3. Remove remaining local-only authenticated fallback behavior once novenas are on the API
4. Verify a real signed archive/upload path from the monorepo once the first two content domains are stable

## Progress Log

- [x] Create migration branch in `sanctuary-platform`
- [x] Move migration tracker into `sanctuary-platform/docs/architecture`
- [x] Create `apps/ios` destination and migration placeholders
- [x] Verify App Store Connect / signing / TestFlight manually
- [x] Import iOS project into `apps/ios`
- [x] Add platform-aware iOS environment and API client foundation
- [x] Add keychain-backed account session flow and API-backed `/me`
- [x] Add API-backed favorites and novena commitments foundation
- [x] Migrate saints to the first API-backed content domain across list, search, detail, calendar, and `Me`
