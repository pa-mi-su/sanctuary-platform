# Android App

This directory is reserved for the native Android Sanctuary app.

Planned stack:

- Kotlin
- Jetpack Compose
- Shared Sanctuary API and Cognito auth model
- GitHub Actions automation isolated to Android-only changes

Current CI behavior:

- PRs only trigger the Android pipeline when `apps/android/**` changes.
- Pushes to `main` only trigger Android release automation when `apps/android/**` changes.
- Until the Android Gradle project exists, the Android workflow succeeds with a placeholder step instead of blocking unrelated releases.
