# Sanctuary Platform

Sanctuary Platform is the shared product foundation for **Sanctuary**, a Catholic prayer companion for daily liturgical life, saints, novenas, prayers, and account-based progress.

This monorepo contains the production platform:

- **Angular web app** for the responsive browser experience
- **Spring Boot API** that owns content, auth integration, user state, and liturgical calculations
- **iOS app** in Swift/SwiftUI
- **Android app** in Kotlin/Jetpack Compose
- **PostgreSQL** schema managed with Flyway
- **AWS deployment path** for web, API, database, and native release pipelines

The important architectural idea is simple: **one Java backend serves all clients**. Web, iOS, and Android use the same API contracts for content, auth, profile data, favorites, novena progress, and calendar behavior.

## Product Scope

Sanctuary currently supports:

- liturgical day, week, and month browsing
- saints by date, saint search, and saint detail pages
- novenas by calendar date, list search, intention search, and detail pages
- prayer browsing and prayer detail pages
- account creation, confirmation, login, refresh, forgot password, and reset password
- user profile and preferences
- favorites for saints, novenas, and prayers
- novena commitments and progress foundations
- shared environment/version display across clients
- Dev, UAT, and production-oriented release tracks

## Tech Stack

### Web

- Angular 21
- TypeScript 5.9
- RxJS
- SCSS
- npm workspaces
- S3 + CloudFront deployment

### API

- Java 21
- Spring Boot 3.5
- Spring MVC
- Spring Security OAuth2 Resource Server
- Spring JDBC
- Flyway
- PostgreSQL driver
- AWS SDK for Cognito integration
- Docker image deployed to ECS

### iOS

- Swift
- SwiftUI
- Xcode project with Dev, UAT, and Prod schemes
- Keychain-backed session storage
- API-backed content/account/progress flows
- TestFlight/App Store Connect pipeline

### Android

- Kotlin
- Jetpack Compose
- Material 3
- Navigation Compose
- ViewModel + lifecycle libraries
- Retrofit + OkHttp
- Gson converter
- Coil for image/SVG loading
- DataStore preferences
- AndroidX Security Crypto
- product flavors for `dev`, `uat`, and `prod`
- Google Play-oriented pipeline currently wired through UAT release paths

### Data, Auth, And Infra

- PostgreSQL for Sanctuary content and app state
- Flyway for schema migrations
- Amazon Cognito for identity
- Sanctuary API for product-specific account linkage and user state
- AWS RDS for production PostgreSQL
- AWS Secrets Manager for production database credentials
- AWS ECR/ECS for API runtime
- AWS S3/CloudFront for web hosting
- GitHub Actions for CI/CD

## Repository Layout

```text
sanctuary-platform/
├── apps/
│   ├── android/    # Native Android app
│   ├── api/        # Spring Boot API
│   ├── ios/        # Native iOS app
│   └── web/        # Angular frontend
├── docs/
│   ├── architecture/
│   └── deployment/
├── scripts/        # Local import/export/restore utilities
├── backups/        # Local database backups
├── docker-compose.yml
├── package.json
└── README.md
```

## How The System Works

### Client Flow

The clients are thin product experiences over the same backend:

1. Web/iOS/Android render the Sanctuary UI.
2. Public content screens call the API without auth.
3. Account screens authenticate through the API-backed Cognito flow.
4. Authenticated requests attach a bearer token.
5. The API validates Cognito JWTs and maps the Cognito identity to Sanctuary's own `users` model.
6. Profile, preferences, favorites, and novena progress are persisted in PostgreSQL.
7. Saints, novenas, prayers, and liturgical calendar data are returned from the backend in the same shape for every platform.

### Backend Flow

The API is the authority for:

- current liturgical day and date ranges
- liturgical anchors such as Easter, Lent, Advent, Pentecost, and transferred feasts
- saint date lookup and search
- novena serving windows and calendar lookup
- prayer and novena content
- user profile, preferences, favorites, and novena commitments
- Cognito-backed auth endpoints

The API starts with the active Spring profile (`local`, `dev`, `uat`, or `prod`), connects to PostgreSQL, runs Flyway validation/migrations, and then serves the public and authenticated endpoints.

### Data Model

PostgreSQL stores:

- platform metadata
- saints and saint sources
- prayers, prayer tags, and prayer source data
- novenas, novena days, intentions, and serving rules
- users and user preferences
- favorites and novena commitments
- account activity/progress foundations

Schema changes live in `apps/api/src/main/resources/db/migration`.

Legacy JSON resources still exist in the iOS bundle as product/source material, but the backend database is the platform source of truth for API-served content.

## API Overview

Base URL is environment-specific. Local API default:

```text
http://localhost:8080
```

Production currently points clients at:

```text
https://sa-d7fe5f77e3bd409caf712e69b701f1e8.ecs.us-east-1.on.aws
```

### Health

| Method | Endpoint | Purpose |
|---|---|---|
| `GET` | `/health` | Simple service health response |
| `GET` | `/actuator/health` | Spring actuator health |

### Authentication

| Method | Endpoint | Purpose |
|---|---|---|
| `POST` | `/auth/register` | Create a Cognito-backed account |
| `POST` | `/auth/confirm` | Confirm registration code |
| `POST` | `/auth/resend-confirmation` | Resend account confirmation code |
| `POST` | `/auth/login` | Login and return tokens/session data |
| `POST` | `/auth/refresh` | Refresh an auth session |
| `POST` | `/auth/forgot-password` | Start password reset |
| `POST` | `/auth/reset-password` | Complete password reset |

### Liturgical Calendar

| Method | Endpoint | Purpose |
|---|---|---|
| `GET` | `/calendar/day/{date}` | Liturgical day for an ISO date |
| `GET` | `/calendar/range?start=&end=` | Liturgical days over a date range |
| `GET` | `/calendar/anchors/{year}` | Computed anchor dates for a year |
| `GET` | `/calendar/novenas/{novenaId}/window/{year}` | Serving window for a novena/year |

### Saints

| Method | Endpoint | Purpose |
|---|---|---|
| `GET` | `/content/saints?month=&day=&lang=` | Saints for a calendar day |
| `GET` | `/content/saints/range?start=&end=&lang=` | Saints grouped by date range |
| `GET` | `/content/saints/search?query=&lang=` | Search saints |
| `GET` | `/content/saints/{slug}?lang=` | Saint detail |

### Prayers

| Method | Endpoint | Purpose |
|---|---|---|
| `GET` | `/content/prayers?query=&lang=` | List/search prayers |
| `GET` | `/content/prayers/{slug}?lang=` | Prayer detail |

### Novenas

| Method | Endpoint | Purpose |
|---|---|---|
| `GET` | `/content/novenas?query=&lang=` | List/search novenas |
| `GET` | `/content/novenas/intentions?query=&lang=` | Search novenas by intention |
| `GET` | `/content/novenas/calendar?start=&end=&lang=` | Novenas grouped by calendar date |
| `GET` | `/content/novenas/{slug}?lang=` | Novena detail and days |

### Authenticated User State

These endpoints require a bearer token.

| Method | Endpoint | Purpose |
|---|---|---|
| `GET` | `/me` | Current Sanctuary profile |
| `PUT` | `/me/preferences` | Update preferences and reminder flags |
| `GET` | `/me/favorites` | List favorites |
| `PUT` | `/me/favorites/{itemType}/{itemId}` | Save favorite |
| `DELETE` | `/me/favorites/{itemType}/{itemId}` | Remove favorite |
| `GET` | `/me/novena-commitments` | List novena commitments |
| `PUT` | `/me/novena-commitments/{novenaId}` | Save novena progress/commitment |
| `DELETE` | `/me/novena-commitments/{novenaId}` | Remove novena commitment |

## Applications

### Web App: `apps/web`

The Angular app is the responsive browser client. It includes:

- home
- liturgical calendar
- saints day/week/month/list/detail flows
- novenas day/week/month/list/intentions/detail flows
- prayers
- auth screens
- Me/profile/about screens
- environment/version display

The web app talks to the API through `SanctuaryApiService` and attaches auth tokens through an HTTP interceptor.

Run locally:

```bash
npm start --workspace web
```

Build:

```bash
npm run build --workspace web
```

### API App: `apps/api`

The Java API owns the backend domain and serves every client.

Important areas:

- `calendar/` for liturgical calculation and novena serving windows
- `content/` for saints, prayers, novenas, and date-based content lookup
- `auth/` for Cognito-backed account flows
- `user/` for profile, preferences, favorites, and novena progress
- `db/migration/` for Flyway schema changes

Run locally:

```bash
cd apps/api
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/opt/openjdk@21/bin:$PATH mvn spring-boot:run
```

Or from the repo root:

```bash
./apps/api/scripts/run-local.sh
```

Test:

```bash
cd apps/api
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/opt/openjdk@21/bin:$PATH mvn -q test
```

### iOS App: `apps/ios`

The iOS app is the native SwiftUI client. It includes:

- Dev, UAT, and Prod schemes
- SwiftUI app shell and feature screens
- API-backed account/session handling
- Keychain session storage
- user progress/favorites foundations
- liturgical, saints, novenas, prayers, search, and Me flows
- TestFlight and App Store Connect release automation

Validate simulator build:

```bash
xcodebuild -project apps/ios/Sanctuary.xcodeproj -scheme Sanctuary-Prod -configuration Debug -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO build
```

### Android App: `apps/android`

The Android app is the native Kotlin/Compose client. It includes:

- Android app module with Gradle Kotlin DSL
- `dev`, `uat`, and `prod` product flavors
- shared API base URL configuration through `BuildConfig`
- Retrofit service matching the backend API
- bearer-token interceptor for authenticated requests
- session repository
- Compose shell, theme, localization, and feature UI foundations
- home card assets and brand assets
- reminder scheduler foundation

Build Dev debug:

```bash
cd apps/android
./gradlew assembleDevDebug
```

Build UAT release:

```bash
cd apps/android
./gradlew assembleUatRelease
```

Build Prod release:

```bash
cd apps/android
./gradlew assembleProdRelease
```

Android currently shares the same Java backend and API contract as web and iOS. Its GitHub pipeline is path-scoped to `apps/android/**` so Android work does not block unrelated web/API/iOS releases.

## Local Development

### Prerequisites

- Node.js and npm
- Java 21
- Docker
- Android Studio/Android SDK for Android work
- Xcode for iOS work

### Start PostgreSQL

```bash
docker compose up -d postgres
```

### Start API

```bash
./apps/api/scripts/run-local.sh
```

### Start Web

```bash
npm start --workspace web
```

### Local URLs

- Web: `http://localhost:4200`
- API: `http://localhost:8080`
- Health: `http://localhost:8080/health`

Recommended local order:

1. PostgreSQL
2. API
3. Web or native client

## Environment Model

The platform uses a branch and environment promotion model:

```text
feature branch -> dev -> uat -> prod -> main
```

Environment concepts:

- `dev`: development validation and internal builds
- `uat`: release-candidate validation
- `prod`: final pre-main promotion gate
- `main`: production deploy/release trigger

The API has Spring profiles:

- `local`
- `dev`
- `uat`
- `prod`

Android has product flavors:

- `dev`
- `uat`
- `prod`

iOS has schemes:

- `Sanctuary-Dev`
- `Sanctuary-UAT`
- `Sanctuary-Prod`

## Deployment Model

### Web

- GitHub Actions builds Angular
- static assets publish to S3
- CloudFront invalidation refreshes production

### API

- GitHub Actions runs Maven tests
- Docker image is built and pushed to ECR
- ECS service is updated
- API starts with `prod` profile
- Flyway validates/runs schema migrations on startup
- `/health` is the load balancer health check

Production DB credential rule:

- prod API reads `SANCTUARY_DB_PASSWORD` directly from the RDS-managed AWS Secrets Manager secret
- do not use an SSM copy such as `/sanctuary/prod/db/password`
- automatic DB secret rotation stays disabled until rotation also triggers an API redeploy

### Database

- RDS PostgreSQL in production
- Flyway controls schema
- content bootstrap/imports are explicit operational steps, not app startup behavior

### iOS

- PRs validate iOS builds
- Dev and UAT builds upload to TestFlight
- production builds upload to App Store Connect from `main`
- final App Store release approval remains manual

### Android

- PRs validate Android when `apps/android/**` changes
- pushes to `dev` and `uat` build Android artifacts for the matching track
- production Google Play release setup is intentionally conservative until Play Console production configuration is ready
- Android pipeline is independent from API, web, and iOS pipelines

## CI/CD Workflows

| Workflow | Scope |
|---|---|
| [`.github/workflows/api-prod-deploy.yml`](.github/workflows/api-prod-deploy.yml) | API tests and production ECS deploy |
| [`.github/workflows/web-prod-deploy.yml`](.github/workflows/web-prod-deploy.yml) | Angular production build and static deploy |
| [`.github/workflows/ios-pipeline.yml`](.github/workflows/ios-pipeline.yml) | iOS validation/TestFlight/App Store Connect flow |
| [`.github/workflows/android-pipeline.yml`](.github/workflows/android-pipeline.yml) | Android validation and environment-scoped artifact upload |

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

## Status

Sanctuary is an active platform build. The strongest current areas are:

- Spring Boot API and PostgreSQL/Flyway model
- liturgical calendar, saints, novenas, and prayers endpoints
- Angular production web app
- iOS native client and release path
- Android native client foundation and UAT-oriented pipeline
- Cognito-backed auth and account state
- AWS production API/web/database deployment path

The product direction is one shared backend, multiple native/browser clients, and a release process that keeps each platform independent while preserving one source of truth for Sanctuary content and user state.
