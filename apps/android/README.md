# Sanctuary Android

`apps/android` is the native Android app for Sanctuary. It is a Kotlin/Jetpack Compose client that uses the shared Java backend for content, auth, profile state, favorites, and novena progress.

## Stack

- Kotlin
- Jetpack Compose
- Material 3
- AndroidX Lifecycle/ViewModel
- Retrofit
- OkHttp
- Gson converter
- Coil
- AndroidX Security Crypto
- Gradle Kotlin DSL

Navigation Compose and DataStore are declared dependencies, but the current runtime uses `rememberSaveable` tab/modal state and encrypted shared preferences rather than those APIs.

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
- prayers and rosary list/detail flows
- Me/profile/about/support/privacy flows
- API-backed favorites and novena progress
- alarm-backed daily/novena reminders, including boot/app-update rescheduling
- verified App Links and sharing for saint, novena, and prayer detail
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

Each flavor points to its configured Sanctuary API URL. Android never talks directly to PostgreSQL or RDS.

Dev defaults to `https://dev-api.mydailysanctuary.com`; UAT and Prod currently use `https://api.mydailysanctuary.com`.

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

Session tokens and language selection use `EncryptedSharedPreferences`. `AndroidReminderScheduler` keeps reminder inputs in a separate private preferences file, schedules 8:00/20:00 novena alarms or an 8:00 general reminder, and restores schedules after reboot or app replacement. App Links accept saint, novena, and prayer paths on both the apex and `www` production hosts.

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

To point Dev debug at a local API, add the following to untracked `local.properties` (the Android emulator reaches the host at `10.0.2.2`):

```properties
SANCTUARY_ANDROID_DEV_API_BASE_URL=http://10.0.2.2:8080
```

This override applies only to `devDebug`; UAT and Prod remain pinned to HTTPS.

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

Without the complete upload-signing set, release variants fall back to the debug key. Those local fallback artifacts are suitable for compilation checks, not Play upload.

`versionCode` is resolved from:

1. `ANDROID_VERSION_CODE`
2. git commit count
3. `GITHUB_RUN_NUMBER` (only reached when the git count is unavailable)
4. `1`

Current `versionName` is defined in [`app/build.gradle.kts`](app/build.gradle.kts).

## Release And CI

The Android workflow is [`../../.github/workflows/android-pipeline.yml`](../../.github/workflows/android-pipeline.yml).

Current behavior:

- PRs validate Android when `apps/android/**` changes
- pushes to `dev` build/upload Dev artifacts
- pushes to `uat` build/upload UAT artifacts
- pushes to `main` build the Prod bundle and can upload it as a draft to the Play alpha/closed-testing track
- final tester rollout and public production release remain manual Play Console actions
- Android is path-scoped so Android work does not block unrelated API, web, or iOS releases

Related docs:

- [`../../docs/android-rollout-plan.md`](../../docs/android-rollout-plan.md)
- [`../../docs/android-play-console-setup.md`](../../docs/android-play-console-setup.md)
