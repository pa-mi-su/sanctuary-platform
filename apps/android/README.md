# Sanctuary Android

`apps/android` is Sanctuary's native Kotlin/Jetpack Compose client. It uses the shared API for all runtime content and account state.

Current release identity:

- production package: `com.pamisu.sanctuary`
- version name: `1.0.15`
- minimum SDK: 26
- target/compile SDK: 36
- Java/Kotlin bytecode target: 17

## Product and interaction model

`MainActivity` hosts one Compose shell with five bottom tabs:

- Home
- Novenas
- Liturgical
- Saints
- Me/account access

Home opens saints, novenas, liturgical calendar, prayers, rosaries, intention search, patronage search, and daily readings. Calendar screens support day/week/month display. Horizontal day navigation uses an `80.dp` threshold converted with `toPx()` at runtime, so the interaction is density-independent; vertical scrolling remains the screen container's responsibility.

`MainViewModel` owns session/content/detail/search/progress state and delegates persistence/network work to `SessionRepository`. It supports:

- registration, confirmation, login, token refresh, password reset, logout, and account deletion
- English, Spanish, and Polish content/UI selection
- saint, novena, prayer, rosary, intention-term, and patronage-term search
- saint/novena/liturgical date ranges
- favorites with optimistic UI and rollback
- novena start/stop/day completion
- reminder preference synchronization
- protected actions that continue after authentication

Structured novena day presentation is centralized in `NovenaDayPresentation.kt`. It suppresses a duplicate fallback body when prayer/reflection/action fields already represent the day's content.

## Source layout

```text
app/src/main/
├── AndroidManifest.xml
├── assets/home_cards/             # SVG home illustrations
├── java/app/sanctuary/android/
│   ├── MainActivity.kt            # Compose shell and feature UI
│   ├── MainViewModel.kt           # Application state/orchestration
│   ├── AndroidReminderScheduler.kt
│   ├── CalendarSwipe.kt
│   ├── NovenaDayPresentation.kt
│   ├── data/                       # Retrofit, DTOs, encrypted session repository
│   └── ui/                         # Localization and theme
└── res/                            # Icons, logo, names, launch theme
```

## Flavors and API mapping

| Flavor | Application ID             | Display/version suffix  | API                                    |
| ------ | -------------------------- | ----------------------- | -------------------------------------- |
| `dev`  | `com.pamisu.sanctuary.dev` | `Sanctuary Dev`, `-dev` | `https://dev-api.mydailysanctuary.com` |
| `uat`  | `com.pamisu.sanctuary.uat` | `Sanctuary UAT`, `-uat` | `https://api.mydailysanctuary.com`     |
| `prod` | `com.pamisu.sanctuary`     | `Sanctuary`             | `https://api.mydailysanctuary.com`     |

`BuildConfig.AUTH_ENABLED` is `true` for every flavor. Cleartext traffic is disabled except when the **DevDebug** override explicitly begins with `http://`.

For local DevDebug API work, set `SANCTUARY_ANDROID_DEV_API_BASE_URL` in `apps/android/local.properties` or the environment. The override is intentionally applied only to the `devDebug` variant; release variants retain their declared HTTPS URL.

Example local value:

```properties
SANCTUARY_ANDROID_DEV_API_BASE_URL=http://10.0.2.2:8080
```

`10.0.2.2` is the Android emulator's route to the host machine.

## API and native session behavior

`data/SanctuaryApiService.kt` mirrors the backend's native endpoints for auth, profile/preferences, favorites, novena commitments, saints, prayers, novenas, intention/patronage terms, and calendar ranges.

`SessionRepository`:

- stores the session in `EncryptedSharedPreferences` using an AndroidX Security `MasterKey`
- stores the preferred language alongside the encrypted session
- sends an ID token (or access-token fallback) only for `/me` requests via `AuthHeaderInterceptor`
- refreshes an expired/rejected session when a refresh token is available
- maps API DTOs into UI models and normalizes favorite metadata for display

Retrofit uses Gson conversion, OkHttp, 15-second connect/read/write timeouts, and basic request logging. API base URLs are normalized with a trailing slash.

## Links, sharing, and reminders

The manifest declares verified HTTPS links for both `mydailysanctuary.com` and `www.mydailysanctuary.com` under:

- `/saints/`
- `/novenas/`
- `/prayers/`

`MainActivity` parses those links and opens the matching API-backed detail. Outbound shares use the same canonical web URLs.

`AndroidReminderScheduler`:

- uses `AlarmManager.setAndAllowWhileIdle`
- schedules active-novena reminders at 08:00 and 20:00 local time when enabled
- otherwise schedules the general daily reminder at 08:00 when enabled
- stores active count and switches in `sanctuary_reminders` shared preferences
- reschedules after delivery, device boot, and app replacement
- creates notification channel `sanctuary-reminders`
- safely skips when notification permission/settings do not allow delivery

The manifest requests Internet, notification, and boot-completed permissions. The app does not request exact-alarm permission; these reminders are intentionally inexact while idle.

## Local build and tests

From this directory:

```bash
./gradlew testDevDebugUnitTest
./gradlew assembleDevDebug
```

Other variants:

```bash
./gradlew assembleUatRelease
./gradlew bundleProdRelease
```

The unit tests cover:

- calendar swipe direction and threshold boundaries
- favorite metadata normalization/state
- novena day fallback/duplicate-content rules

Instrumented Compose dependencies are configured, but no `androidTest` source files currently exist.

## Signing and version codes

Release signing reads:

- `ANDROID_UPLOAD_KEYSTORE_PATH`
- `ANDROID_UPLOAD_KEYSTORE_PASSWORD`
- `ANDROID_UPLOAD_KEY_ALIAS`
- `ANDROID_UPLOAD_KEY_PASSWORD`

When those values are absent, Gradle signs local release builds with the debug key. That fallback is for local buildability, not store distribution. GitHub Actions decodes the protected upload keystore before a store build.

Gradle resolves `versionCode` in this order:

1. `ANDROID_VERSION_CODE`
2. Git commit count
3. `GITHUB_RUN_NUMBER`
4. `1`

The workflow queries Google Play and selects a code above existing uploaded artifacts before building, preventing reuse of a Play version code.

## CI and Google Play behavior

`.github/workflows/android-pipeline.yml` behaves as follows:

| Trigger                                                                  | Result                                                                                                                 |
| ------------------------------------------------------------------------ | ---------------------------------------------------------------------------------------------------------------------- |
| PR to `dev`, `uat`, `prod`, or `main` with Android code/workflow changes | `assembleDevDebug` Gradle validation                                                                                   |
| Push to `dev` with Android code changes                                  | signed `devRelease` AAB artifact; Play `internal` upload with `draft` status when credentials exist                    |
| Push to `uat` with Android code changes                                  | signed `uatRelease` AAB artifact; Play `alpha` upload with `draft` status when credentials exist                       |
| Push to `main` with Android code changes                                 | signed production AAB artifact; production package uploaded to Play `alpha` with `draft` status when credentials exist |
| Manual dispatch                                                          | selected branch/environment job according to the workflow conditions                                                   |

The `main` job does **not** publish directly to the public production track. It stages the production package in the closed-testing `alpha` track, and Google Play Console remains the place to add release notes, review warnings, submit changes, promote tracks, and start rollout.

Markdown-only changes under `apps/android` do not start this workflow. Updating the workflow YAML can validate it on a PR, but a workflow-only merge does not upload an AAB.

## Operational references

- `../../docs/android-play-console-setup.md`
- `../../docs/android-rollout-plan.md`
- `../../.github/workflows/android-pipeline.yml`
