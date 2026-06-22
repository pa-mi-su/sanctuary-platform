# Sanctuary Platform

![Angular](https://img.shields.io/badge/Web-Angular%2021-c3002f)
![Spring Boot](https://img.shields.io/badge/API-Spring%20Boot%203.5-6db33f)
![iOS](https://img.shields.io/badge/iOS-SwiftUI-111111)
![Android](https://img.shields.io/badge/Android-Jetpack%20Compose-3ddc84)
![PostgreSQL](https://img.shields.io/badge/Data-PostgreSQL-336791)
![AWS](https://img.shields.io/badge/Infra-AWS-ff9900)

Sanctuary is a Catholic prayer companion for daily liturgical life: saints, novenas, prayers, calendar-aware devotional content, saved progress, and mobile notifications.

This repository is the shared production platform behind the product. It contains the Angular web app, Spring Boot API, native iOS app, native Android app, PostgreSQL/Flyway schema, and deployment path for AWS-backed environments.

The core architecture is intentionally simple: one backend owns the domain, and every client uses the same API contracts.

```text
Angular Web      SwiftUI iOS      Compose Android
     \              |                  /
      \             |                 /
       +------ Spring Boot API ------+
                    |
          PostgreSQL + Flyway
                    |
        Cognito, Firebase, AWS
```

## What Sanctuary Does

Sanctuary currently supports:

- Liturgical day, week, month, and calendar-range browsing.
- Saint date lookup, search, and saint detail pages.
- Novena browsing, calendar serving windows, intention search, and detail pages.
- Prayer browsing and prayer detail pages.
- Account registration, confirmation, login, refresh, forgot password, and reset password.
- Profile, preferences, favorites, and novena commitment foundations.
- Shared version/build/environment display across clients.
- Anonymous and signed-in mobile app activity tracking.
- Push-ready device registration for iOS and Android.
- Admin-only operations dashboard for users, devices, push readiness, notification drafts, and delivery logs.

## Platform Highlights

| Area | Current Capability |
|---|---|
| Shared API | One Java/Spring API serves web, iOS, and Android. |
| Content | Saints, prayers, novenas, intentions, and liturgical calendar data come from PostgreSQL. |
| Auth | Cognito-backed account flows with Sanctuary-owned user records. |
| Mobile | Native SwiftUI and Jetpack Compose clients share backend contracts. |
| Push | Firebase Cloud Messaging registration and admin-triggered notification sends. |
| Operations | `/sanctuary-ops` dashboard for admin access, metrics, device visibility, and notifications. |
| Security | Admin APIs require authenticated enabled admins, throttle sensitive routes, and reject unsafe bad-origin browser requests. |
| Deployment | AWS-oriented API, web, database, and native release paths. |

## Current App Versions

| Client | Version |
|---|---|
| Web | `1.0.14` |
| iOS | `1.0.13` build `2` |
| Android | `1.0.13` |

## Repository Layout

```text
sanctuary-platform/
├── apps/
│   ├── android/    # Native Android app, Kotlin, Jetpack Compose
│   ├── api/        # Spring Boot API, Flyway migrations, backend tests
│   ├── ios/        # Native iOS app, Swift, SwiftUI, Xcode schemes
│   └── web/        # Angular app and operations dashboard
├── docs/
│   ├── architecture/
│   └── deployment/
├── scripts/        # Local database utilities and content helpers
├── backups/        # Local database backups
├── docker-compose.yml
├── package.json
└── README.md
```

## Applications

### Web: `apps/web`

Angular 21 browser client with:

- Home, liturgical, saints, novenas, prayers, auth, Me, and About views.
- Responsive app shell and shared API client.
- Sanctuary favicon, web manifest, and share-preview generation.
- Admin-only operations dashboard at `/sanctuary-ops`.

Useful commands:

```bash
npm start --workspace web
npm run build --workspace web
npm test --workspace web -- --watch=false
```

### API: `apps/api`

Spring Boot API with:

- Public content endpoints for calendar, saints, prayers, novenas, and intentions.
- Cognito-backed auth endpoints.
- Authenticated `/me` profile, preferences, favorites, and novena progress endpoints.
- Mobile activity endpoints for anonymous and signed-in app usage.
- Admin endpoints for operations metrics, admin access management, notification drafts, notification sends, and delivery logs.
- Flyway-managed PostgreSQL schema.

Useful commands:

```bash
./apps/api/scripts/run-local.sh

cd apps/api
mvn -q test
```

### iOS: `apps/ios`

Native SwiftUI app with:

- Dev Local, Dev, UAT, and Prod schemes.
- API-backed account/session handling.
- Keychain-backed session storage.
- Firebase initialization and push-token registration.
- Anonymous activity reporting before login and signed-in activity reporting after login.
- Shared version/build/environment display.

Useful build command:

```bash
xcodebuild \
  -project apps/ios/Sanctuary.xcodeproj \
  -scheme "Sanctuary-Dev Local" \
  -configuration Debug-Dev \
  -destination "generic/platform=iOS Simulator" \
  build
```

### Android: `apps/android`

Native Kotlin/Jetpack Compose app with:

- `dev`, `uat`, and `prod` product flavors.
- Retrofit/OkHttp API client.
- DataStore preferences and encrypted session storage.
- Firebase Messaging foreground/background notification handling.
- Anonymous and signed-in app activity reporting.
- Dev debug builds that default to the Android emulator local API (`http://10.0.2.2:8080`) unless overridden.
- UAT/prod builds that keep their remote API defaults.

Useful commands:

```bash
cd apps/android
./gradlew :app:assembleDevDebug
./gradlew :app:installDevDebug
./gradlew :app:assembleUatRelease
./gradlew :app:assembleProdRelease
```

Override the Android dev API base URL when needed:

```bash
./gradlew :app:installDevDebug -PSANCTUARY_ANDROID_API_BASE_URL=http://10.0.2.2:8080
```

## Operations Dashboard

The admin dashboard lives at:

```text
/sanctuary-ops
```

It is built for operator visibility, not public marketing. It currently provides:

- Admin access list with enable/disable checkboxes.
- Server-side self-demotion protection.
- Audit events for admin access changes.
- User and sign-in metrics.
- Anonymous app usage metrics.
- Recent app installs with signed-in/anonymous state, platform, app version, app language, push state, first seen, and last seen.
- Deduped device display by FCM token so a login transition does not double-count one physical install.
- Push-ready device counts split by iOS and Android.
- Notification draft creation.
- Notification send flow to reachable devices.
- Firebase handoff delivery log with success/failure reason where available.

Admin access is enforced by the API. The frontend guard improves UX, but the backend is the source of truth.

## Security Model

Security work currently in place:

- Admin API requires authentication.
- Admin API requires `admin_users.enabled = true`.
- Unauthenticated admin calls return `401`.
- Authenticated non-admin calls return `403`.
- Admin self-demotion is blocked server-side.
- Admin access changes write audit events.
- Admin routes have throttling.
- Unsafe browser requests from untrusted origins are rejected.
- Session cookies are `HttpOnly`; production config uses secure-cookie behavior.
- If auth is accidentally disabled, `/admin/**` and `/me/**` fail closed.
- No private keys or service-account JSON should be committed; Firebase server credentials belong in secrets/env.

Production hardening still belongs at the edge and environment layer:

- AWS WAF or CloudFront/ALB rate limits for `/admin/*`, `/auth/*`, and `/me/*`.
- HSTS.
- Content Security Policy.
- `frame-ancestors` or `X-Frame-Options`.
- `X-Content-Type-Options: nosniff`.
- Referrer policy.
- Strict production Cognito issuer/client/audience configuration.

## API Overview

Local API default:

```text
http://localhost:8080
```

Common endpoint groups:

| Group | Example Endpoints |
|---|---|
| Health | `GET /health`, `GET /actuator/health` |
| Auth | `POST /auth/register`, `/auth/login`, `/auth/refresh`, `/auth/reset-password` |
| Calendar | `GET /calendar/day/{date}`, `/calendar/range`, `/calendar/anchors/{year}` |
| Saints | `GET /content/saints/search`, `/content/saints/{slug}` |
| Prayers | `GET /content/prayers`, `/content/prayers/{slug}` |
| Novenas | `GET /content/novenas`, `/content/novenas/calendar`, `/content/novenas/{slug}` |
| User State | `GET /me`, `PUT /me/preferences`, `/me/favorites`, `/me/novena-commitments` |
| App Activity | `POST /app/activity`, `POST /me/activity` |
| Admin | `GET /admin/users`, `/admin/notifications`, `/admin/notifications/deliveries` |

## Local Development

Prerequisites:

- Node.js and npm.
- Java 21.
- Docker.
- Xcode for iOS work.
- Android Studio and Android SDK for Android work.

Start local PostgreSQL:

```bash
docker compose up -d postgres
```

Start local API:

```bash
./apps/api/scripts/run-local.sh
```

Start local web:

```bash
npm start --workspace web
```

Local URLs:

| Service | URL |
|---|---|
| Web | `http://localhost:4200` |
| API | `http://localhost:8080` |
| Health | `http://localhost:8080/health` |
| Operations | `http://localhost:4200/sanctuary-ops` |

Recommended local order:

1. PostgreSQL.
2. API.
3. Web, iOS simulator, Android emulator, or physical device.

## Environment Model

Promotion flow:

```text
feature branch -> dev -> uat -> prod -> main
```

Runtime environments:

| Layer | Local/Dev | UAT | Production |
|---|---|---|---|
| API | Spring profiles `local`, `dev` | `uat` | `prod` |
| Web | Angular local/dev config | UAT deploy | production CloudFront/S3 |
| iOS | `Sanctuary-Dev Local`, `Sanctuary-Dev` | `Sanctuary-UAT` | `Sanctuary-Prod` |
| Android | `dev` flavor | `uat` flavor | `prod` flavor |

## Deployment Model

### Web

- GitHub Actions builds Angular.
- Static assets publish to S3.
- CloudFront invalidation refreshes the hosted app.

### API

- GitHub Actions runs Maven tests.
- Docker image is built and pushed to ECR.
- ECS service is updated.
- Flyway validates/runs schema migrations on startup.
- `/health` is the load balancer health check.

Production DB credential rule:

- The prod API reads `SANCTUARY_DB_PASSWORD` directly from the RDS-managed AWS Secrets Manager secret.
- Do not use an SSM copy such as `/sanctuary/prod/db/password`.
- Automatic DB secret rotation stays disabled until rotation also triggers an API redeploy.

### Database

- PostgreSQL for content, account state, devices, admin operations, and notification logs.
- Flyway owns schema migration.
- Content bootstrap/imports are explicit operational steps, not app startup behavior.

### iOS

- PRs validate iOS builds.
- Dev and UAT builds upload to TestFlight.
- Production builds upload to App Store Connect from `main`.
- Final App Store release approval remains manual.

### Android

- PRs validate Android when `apps/android/**` changes.
- Pushes to `dev` and `uat` build Android artifacts for the matching track.
- Production Google Play release setup remains conservative until Play Console production configuration is ready.
- Android pipeline is independent from API, web, and iOS pipelines.

## Verification Checklist

Use this before merging a branch that touches app/API behavior:

```bash
cd apps/api && mvn -q test
npm run build --workspace web
npm test --workspace web -- --watch=false
cd apps/android && ./gradlew :app:assembleDevDebug
xcodebuild -project apps/ios/Sanctuary.xcodeproj -scheme "Sanctuary-Dev Local" -configuration Debug-Dev -destination "generic/platform=iOS Simulator" build
```

Security spot checks used for this branch:

```bash
curl -i http://127.0.0.1:8080/actuator/health
curl -i http://127.0.0.1:8080/admin/users?limit=1
curl -i -X POST http://127.0.0.1:8080/app/activity \
  -H "Origin: https://attacker.example" \
  -H "Content-Type: application/json" \
  --data '{"anonymousDeviceId":"audit-device","eventType":"app_open","platform":"ios","language":"en"}'
```

Expected results:

- Health returns `200`.
- Unauthenticated admin request returns `401`.
- Bad-origin unsafe request returns `403`.

## Useful Docs

- [`docs/architecture/platform-reset-architecture.md`](docs/architecture/platform-reset-architecture.md)
- [`docs/architecture/local-development.md`](docs/architecture/local-development.md)
- [`docs/architecture/postgres-schema.md`](docs/architecture/postgres-schema.md)
- [`docs/architecture/liturgical-engine-plan.md`](docs/architecture/liturgical-engine-plan.md)
- [`docs/architecture/deployment-and-pipelines.md`](docs/architecture/deployment-and-pipelines.md)
- [`docs/deployment/api-prod-deploy-setup.md`](docs/deployment/api-prod-deploy-setup.md)
- [`docs/deployment/rds-production-bootstrap.md`](docs/deployment/rds-production-bootstrap.md)
- [`docs/deployment/cognito-auth-setup.md`](docs/deployment/cognito-auth-setup.md)
- [`docs/android-rollout-plan.md`](docs/android-rollout-plan.md)
- [`docs/android-play-console-setup.md`](docs/android-play-console-setup.md)
- [`apps/api/README.md`](apps/api/README.md)
- [`apps/web/README.md`](apps/web/README.md)
- [`apps/ios/README.md`](apps/ios/README.md)
- [`apps/android/README.md`](apps/android/README.md)

## Current Status

Sanctuary is an active full-stack product build with:

- A shared Spring Boot/PostgreSQL backend.
- A production-oriented Angular web app.
- Native iOS and Android clients.
- Cognito-backed account state.
- Firebase-backed push notification plumbing.
- Admin-only operations tooling for metrics, devices, notifications, and admin access.
- AWS deployment paths for API, web, database, and native release workflows.

The product direction remains one source of truth for Catholic content and user state, delivered through focused native and browser clients with a conservative, auditable release process.
