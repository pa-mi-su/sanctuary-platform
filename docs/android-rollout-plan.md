# Android Rollout Plan

This document tracks the Android path for Sanctuary from scaffold to release.

## Current status

The repository now includes:

- `apps/android` native Android app scaffold
- Kotlin + Jetpack Compose project files
- a path-scoped GitHub Actions workflow for Android only

The Android workflow is intentionally conservative until the Gradle wrapper and signing setup are added.

## Phase 1: Foundation

1. Check in the Gradle wrapper
2. Open the project in Android Studio
3. Confirm local build and emulator launch
4. Add app icons, adaptive icon assets, and splash polish
5. Add `dev`, `uat`, and `prod` flavor strategy

## Phase 2: Shared platform integration

1. Environment config
   - API base URL per environment
   - Cognito user pool/client values per environment
2. Networking layer
   - Retrofit or Ktor client
   - JSON model mapping
   - auth interceptor
3. Session model
   - login
   - register
   - confirm
   - forgot/reset password
   - refresh-token renewal

## Phase 3: First vertical slice

1. Auth shell
2. Home shell
3. Saints or novenas list
4. Detail flow

The first slice should prove:

- environment loading
- authenticated API requests
- list/detail UI pattern
- session persistence

## Phase 4: Core Sanctuary feature parity

1. Saints
2. Novenas
3. Intentions
4. Prayers
5. Liturgical calendar
6. Me page
7. Favorites
8. Novena progress
9. Reminder preferences

## Phase 5: Play Console release path

1. Internal testing track
2. Closed testing track
3. Store listing metadata
4. Data safety / privacy disclosures
5. Production release

## CI/CD target model

Android should stay independent in GitHub:

- Android PR validation runs only for `apps/android/**`
- Android release automation runs only for Android changes
- API, web, iOS, and Android should not force each other to release

## Recommended next implementation step

The next best concrete step is:

1. add the Gradle wrapper
2. open in Android Studio
3. verify local launch
4. then implement auth foundation before broader content screens
