# Sanctuary Android

`apps/android` is the native Android app for Sanctuary. It is a Kotlin/Jetpack Compose client that uses the shared Java backend for content, auth, profile state, favorites, and novena progress.

## Stack

- Kotlin
- Jetpack Compose
- Material 3
- Navigation Compose
- AndroidX Lifecycle/ViewModel
- Retrofit
- OkHttp
- Gson converter
- Coil
- DataStore preferences
- AndroidX Security Crypto
- Gradle Kotlin DSL

## Structure

```text
apps/android/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── java/app/sanctuary/android/
│       │   ├── data/       # Retrofit API, models, session storage
│       │   ├── ui/         # Localization and theme
│       │   ├── MainActivity.kt
│       │   └── MainViewModel.kt
│       ├── assets/         # Home card SVG assets
│       └── res/            # App icons, logo, values
├── build.gradle.kts
├── gradlew
├── settings.gradle.kts
└── README.md
```

## Product Areas

The Android app includes:

- home
- auth/account access
- liturgical calendar
- saints day/week/month/search/detail flows
- novenas day/week/month/search/intentions/detail flows
- prayers list/detail
- Me/profile/about/support/privacy flows
- favorites and novena progress foundations
- reminder scheduler foundation
- environment/version display

## Flavors And Environment

Android defines three product flavors in [`app/build.gradle.kts`](app/build.gradle.kts):

- `dev`
- `uat`
- `prod`

Each flavor sets:

- app name
- `BuildConfig.ENVIRONMENT`
- `BuildConfig.API_BASE_URL`
- `BuildConfig.AUTH_ENABLED`

All current flavors point to the shared Sanctuary API URL. Android never talks directly to PostgreSQL or RDS.

## API And Auth

Retrofit endpoints live in [`app/src/main/java/app/sanctuary/android/data/SanctuaryApiService.kt`](app/src/main/java/app/sanctuary/android/data/SanctuaryApiService.kt).

The service covers:

- auth registration/login/refresh/password reset
- `/me`
- favorites
- novena commitments
- saints
- prayers
- novenas
- liturgical calendar ranges

Authenticated calls attach bearer tokens through `AuthHeaderInterceptor`.

Session persistence lives in [`app/src/main/java/app/sanctuary/android/data/SessionRepository.kt`](app/src/main/java/app/sanctuary/android/data/SessionRepository.kt).

## Local Build

From this directory:

```bash
./gradlew assembleDevDebug
```

Other useful builds:

```bash
./gradlew assembleUatRelease
./gradlew assembleProdRelease
```

From the repo root:

```bash
cd apps/android
./gradlew assembleDevDebug
```

If Android Studio rewrites `local.properties`, keep it local-only.

## Signing And Versioning

Release signing uses environment variables when available:

- `ANDROID_UPLOAD_KEYSTORE_PATH`
- `ANDROID_UPLOAD_KEYSTORE_PASSWORD`
- `ANDROID_UPLOAD_KEY_ALIAS`
- `ANDROID_UPLOAD_KEY_PASSWORD`

`versionCode` is resolved from:

1. `ANDROID_VERSION_CODE`
2. git commit count
3. `GITHUB_RUN_NUMBER`
4. `1`

Current `versionName` is defined in [`app/build.gradle.kts`](app/build.gradle.kts).

## Release And CI

The Android workflow is [`../../.github/workflows/android-pipeline.yml`](../../.github/workflows/android-pipeline.yml).

Current behavior:

- PRs validate Android when `apps/android/**` changes
- pushes to `dev` build/upload Dev artifacts
- pushes to `uat` build/upload UAT artifacts
- production Google Play release setup is intentionally conservative until Play Console production configuration is ready
- Android is path-scoped so Android work does not block unrelated API, web, or iOS releases

Related docs:

- [`../../docs/android-rollout-plan.md`](../../docs/android-rollout-plan.md)
- [`../../docs/android-play-console-setup.md`](../../docs/android-play-console-setup.md)
