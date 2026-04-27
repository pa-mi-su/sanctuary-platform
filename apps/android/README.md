# Sanctuary Android

This is the native Android app foundation for Sanctuary.

## Planned stack

- Kotlin
- Jetpack Compose
- Same Sanctuary API and Cognito auth model as iOS and web
- GitHub Actions automation scoped only to Android changes

## Current status

The project is scaffolded with:

- Gradle Kotlin DSL project files
- Android app module
- Compose app shell and theme
- A simple home screen that maps the intended Sanctuary sections

The next implementation slices should be:

1. Gradle wrapper check-in
2. Environment configuration for `dev`, `uat`, and `prod`
3. API client + auth/session refresh flow
4. Login/register/confirm/reset password
5. Home, saints, novenas, intentions, prayers, liturgical, and Me flows
6. Favorites, novena progress, and reminders

## CI/CD

Android is intended to stay independent from API, web, and iOS releases.
The GitHub workflow should only run when `apps/android/**` changes.

