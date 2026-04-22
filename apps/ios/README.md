## iOS App Workspace

This directory is the home of the native Sanctuary iOS app inside the `sanctuary-platform` monorepo.

The first structural import has been completed. The legacy Xcode project and app source tree now live here so we can migrate carefully and preserve:

- build stability
- Xcode project references
- signing configuration
- environment-specific bundle identifiers
- a clean path from legacy local-data architecture to API-backed platform architecture

## Migration Rules

- Move first, rewrite second.
- Do not copy partial feature folders into this directory.
- Do not leave the app half on bundled JSON and half on API for the same domain.
- Treat `apps/api` as the long-term source of truth for content and user state.
- Keep release/signing verification in lockstep with structural migration.

## Planned Contents

This directory now contains the native iOS project and source tree in preserved relative layout:

```text
apps/ios/
  Sanctuary.xcodeproj
  Sanctuary/
  README.md
```

## What Has Been Verified

- the standalone project was imported without changing its internal relative layout
- the imported project still exposes:
  - `Sanctuary-Prod`
  - `Sanctuary-Dev`
  - `Sanctuary-UAT`
- simulator builds succeed from the monorepo for prod, dev, and UAT when validated via `xcodebuild`

## Environment Routing

- `Sanctuary-Prod` points to the production API.
- `Sanctuary-Dev` points to `http://localhost:8080` by default.
- `Sanctuary-UAT` points to `http://localhost:8080` by default.

Important:

- iOS never talks directly to PostgreSQL or RDS.
- Dev and UAT now point to a local backend by default, and that backend can use your local Postgres instance.
- For real-device testing, `localhost` means the device itself, not your Mac. In that case, override `SANCTUARY_API_BASE_URL` to your Mac's LAN IP or a tunnel URL.

## Remaining Verification

Continue using the Apple-side release checklist in:

- `docs/deployment/ios-app-store-verification-checklist.md`

Also keep the migration tracker up to date:

- `docs/architecture/ios-to-platform-migration-tracker.md`

## Immediate Next Step

The next safe step is not another file move. It is to begin the application-layer migration:

- introduce shared environment configuration for iOS inside the monorepo
- wire real platform auth and account flows
- replace legacy local repositories domain by domain with API-backed implementations
