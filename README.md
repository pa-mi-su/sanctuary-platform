# Sanctuary Platform

Sanctuary is a Catholic daily companion delivered as an Angular web app, a SwiftUI iOS app, and a Jetpack Compose Android app. All three clients use one Spring Boot API and one PostgreSQL data model for liturgical content, accounts, favorites, preferences, and novena progress.

This README describes the code at the current branch tip. Files under `docs/architecture` include historical plans and migration records; executable code, Flyway migrations, and GitHub Actions are authoritative when a planning document differs.

## Repository map

```text
sanctuary-platform/
├── apps/
│   ├── api/       # Java 21 / Spring Boot 3.5.6 API
│   ├── web/       # Angular 21 browser client
│   ├── ios/       # SwiftUI native client
│   └── android/   # Kotlin / Jetpack Compose native client
├── docs/          # Architecture, security, deployment, and release records
├── scripts/       # Explicit content import/export utilities
├── .github/
│   ├── workflows/ # CI/CD and DEV lifecycle automation
│   └── scripts/   # Guarded DEV AWS start/stop implementation and tests
├── docker-compose.yml
├── package.json
└── package-lock.json
```

Component details:

- [Web README](apps/web/README.md)
- [API README](apps/api/README.md)
- [iOS README](apps/ios/README.md)
- [Android README](apps/android/README.md)

## Current product behavior

The clients expose the same core product, with platform-native presentation:

- English, Spanish, and Polish UI/content selection
- day, week, and month liturgical browsing
- saints by date, search, detail, patronage, favorites, and shared links
- novenas by date, search, intention, detail, nine-day progress, favorites, and shared links
- prayer and rosary search/detail, favorites, and shared links
- daily readings opened from USCCB URLs supplied by the calendar API
- Cognito-backed registration, confirmation, login, refresh, forgot-password, and reset-password flows
- profile, language/time-zone preferences, reminder settings, favorites, commitments, and account deletion
- optional native daily/novena reminders on iOS and Android
- verified/universal links for `https://mydailysanctuary.com/{saints|novenas|prayers}/{slug}`

## Architecture and request flows

### Public content

1. A client selects an environment-specific API base URL.
2. Public calendar and `/content/**` requests require no account.
3. The API validates dates, ranges, search text, and `lang` (`en`, `es`, or `pl`).
4. JDBC repositories read PostgreSQL content populated through explicit operational imports.
5. Calendar services calculate liturgical days, movable anchors, transferred feasts, and novena serving windows.
6. DTOs are returned to each client; clients own presentation and local navigation.

The API database, not the native bundled JSON files, is the runtime source of truth. JSON under `apps/ios/Sanctuary/Resources` remains source/reference material and preview support; app startup does not import it.

### Account and protected state

1. Registration and password operations pass through the API to Amazon Cognito.
2. Native login endpoints return tokens. iOS stores the session in Keychain; Android uses encrypted shared preferences.
3. Web login/reset endpoints set HttpOnly cookies. The Angular app does not retain bearer or refresh tokens in browser storage.
4. When auth is enabled, Spring Security validates the Cognito issuer and audience/client ID. `/me/**` requires authentication; health, auth, calendar, and content endpoints remain public.
5. The API maps the Cognito subject to a Sanctuary `users` row and persists profile preferences, favorites, commitments, and activity in PostgreSQL.
6. A rejected native request can refresh the token and retry. The web interceptor sends credentials and performs one `/auth/web/refresh` retry after a non-auth API request returns `401`.

When `sanctuary.auth.enabled=false`, the security chain permits all requests. Deployed environments must therefore explicitly provide the intended `SANCTUARY_AUTH_ENABLED` value; the base, dev, and prod property defaults are `false`, while the local profile defaults to `true`.

### Share and deep-link flow

The web production build runs `apps/web/scripts/generate-share-previews.mjs` after Angular compilation. It fetches English saint, novena, and prayer content from the configured API, then writes static Open Graph preview documents for share URLs and a direct `/privacy` document. CloudFront serves those files with explicit metadata. The apex-domain URLs open native detail screens through the iOS associated domain and Android verified app links when a matching app is installed; both native parsers also accept `www` URLs delivered to them.

### Reminders

- iOS uses `UNUserNotificationCenter`: active novena reminders are scheduled at 08:00 and 20:00 local time; the general daily reminder uses 08:00.
- Android uses `AlarmManager.setAndAllowWhileIdle`, stores reminder state, reschedules after delivery, boot, and app replacement, and uses the same 08:00/20:00 behavior.
- Reminder switches are stored in the account profile. Native scheduling occurs only when notification permission is available.

## Technology and runtime versions

| Area     | Current implementation                                                                               |
| -------- | ---------------------------------------------------------------------------------------------------- |
| Web      | Angular 21.2, TypeScript 5.9, RxJS 7.8, SCSS, Vitest 4                                               |
| API      | Java 21 exactly, Spring Boot 3.5.6, Spring MVC/Security/JDBC, Flyway, Maven                          |
| Database | PostgreSQL 17 locally; PostgreSQL on AWS RDS when deployed                                           |
| iOS      | Swift 5 language mode, SwiftUI, iOS 16.6 deployment target, version 1.0.15                           |
| Android  | Kotlin, Compose/Material 3, Java/Kotlin target 17, min SDK 26, target/compile SDK 36, version 1.0.15 |
| AWS      | S3/CloudFront web hosting, ECR/ECS API runtime, RDS, Secrets Manager, Cognito                        |

## Environment matrix

| Environment | Web host                                  | API                                    | iOS identity                  | Android identity            |
| ----------- | ----------------------------------------- | -------------------------------------- | ----------------------------- | --------------------------- |
| Local       | `http://localhost:4200`                   | `http://localhost:8080`                | Run-scheme override as needed | DevDebug override as needed |
| DEV         | `https://dev.mydailysanctuary.com`        | `https://dev-api.mydailysanctuary.com` | `com.pamisu.Sanctuary.dev`    | `com.pamisu.sanctuary.dev`  |
| UAT         | no separate web/API host in client config | production API                         | `com.pamisu.Sanctuary.uat`    | `com.pamisu.sanctuary.uat`  |
| Production  | `https://mydailysanctuary.com`            | `https://api.mydailysanctuary.com`     | `com.pamisu.Sanctuary`        | `com.pamisu.sanctuary`      |

UAT native builds intentionally use the production API. The `application-uat.yml` API profile exists but there is no UAT API deployment workflow in this repository.

Web API resolution is deterministic:

- `?api=local`, `?api=dev`, or `?api=prod` overrides host detection.
- localhost/127.0.0.1 uses the local API.
- `dev.mydailysanctuary.com` uses the DEV API.
- every other host uses production.

## Local development

### Prerequisites

- Node.js with npm 10-compatible tooling
- Java 21 (Maven Enforcer rejects other major versions)
- Docker Desktop or another Docker Compose runtime
- Xcode for iOS builds
- Android SDK/Android Studio for Android builds

### Configure and start PostgreSQL

Create a root `.env` (it is gitignored) and provide both Compose and API values. The helper requires `SANCTUARY_DB_URL`, `SANCTUARY_DB_USERNAME`, and `SANCTUARY_DB_PASSWORD`; Compose consumes `POSTGRES_DB`, `POSTGRES_USER`, and `POSTGRES_PASSWORD`.

```bash
docker compose up -d postgres
```

### Start the API

```bash
./apps/api/scripts/run-local.sh
```

The helper finds Java 21, loads the root `.env`, and runs Spring Boot with the `local` profile.

### Start the web app

In another terminal:

```bash
npm ci
npm start --workspace web
```

Local endpoints:

- Web: `http://localhost:4200`
- API: `http://localhost:8080`
- Health: `http://localhost:8080/health`
- Actuator health: `http://localhost:8080/actuator/health`

### Core verification commands

```bash
# Web unit tests
npm test --workspace web -- --watch=false

# API tests
(cd apps/api && mvn -q test)

# Android unit tests and DevDebug assembly
(cd apps/android && ./gradlew testDevDebugUnitTest assembleDevDebug)

# iOS platform mapping and unsigned simulator build
swiftc \
  apps/ios/Sanctuary/Core/Application/PlatformConfiguration.swift \
  apps/ios/Scripts/verify-platform-configuration.swift \
  -o /tmp/verify-sanctuary-platform
/tmp/verify-sanctuary-platform
xcodebuild -project apps/ios/Sanctuary.xcodeproj \
  -scheme Sanctuary-Prod -configuration Debug \
  -destination 'generic/platform=iOS Simulator' \
  CODE_SIGNING_ALLOWED=NO build

# DEV lifecycle script tests
bash .github/scripts/test-dev-environment-control.sh
```

`npm run build --workspace web` is not a fully offline build: after Angular compilation it generates previews by reading the configured content API.

## API surface

The full contract and validation limits are documented in [apps/api/README.md](apps/api/README.md). The route families are:

| Access          | Routes                                                                                  |
| --------------- | --------------------------------------------------------------------------------------- |
| Public health   | `GET /health`, `GET /actuator/health`                                                   |
| Public auth     | native and web session operations under `/auth/**`                                      |
| Public calendar | `/calendar/day`, `/calendar/range`, `/calendar/anchors`, novena serving windows         |
| Public content  | saints, prayers, novenas, intention terms, and patronage terms under `/content/**`      |
| Authenticated   | profile/account deletion, preferences, favorites, and novena commitments under `/me/**` |

## PostgreSQL model

Flyway migrations `V1` through `V15` are authoritative and create/evolve these groups:

- platform metadata
- saints, localized fields, tags, legacy patronage strings, and sources
- prayers and tags
- novenas, days, legacy intention strings, tags, and serving rules
- users, profile fields, preferences, favorites, commitments, and activity events
- deletion audit records containing a one-way email hash rather than the deleted account row
- normalized multilingual intention terms/aliases linked to novenas and saints
- normalized multilingual patronage terms/aliases linked to saints

Migrations run at API startup. Content import is never an implicit startup action.

## Promotion and CI/CD

The repository uses the operational promotion sequence:

```text
feature branch -> dev -> uat -> prod -> main
```

Pull requests validate the app areas changed. Deployment/upload happens on selected branch pushes:

| Workflow           | PR validation                               | Push/deployment behavior                                                                              |
| ------------------ | ------------------------------------------- | ----------------------------------------------------------------------------------------------------- |
| `Web Dev Pipeline` | PR to `dev` when web/root npm inputs change | `dev` deploys to DEV S3/CloudFront                                                                    |
| `Web Pipeline`     | PR to `dev`, `uat`, `prod`, or `main`       | `main` deploys production S3/CloudFront                                                               |
| `API Dev Pipeline` | PR to `dev`                                 | `dev` tests, builds/pushes ECR, and updates DEV ECS                                                   |
| `API Pipeline`     | PR to `dev`, `uat`, `prod`, or `main`       | `main` tests, builds/pushes ECR, updates production ECS, and verifies public health                   |
| `iOS Pipeline`     | PR to `dev`, `uat`, `prod`, or `main`       | `dev` -> Dev TestFlight; `uat` -> UAT TestFlight; `main` -> App Store Connect production build        |
| `Android Pipeline` | PR to `dev`, `uat`, `prod`, or `main`       | `dev` -> internal draft; `uat` -> alpha draft; `main` -> alpha draft, plus downloadable AAB artifacts |

The native store workflows upload artifacts; App Store and Google Play review/release remain store-console operations. Android `main` currently uploads the production package to the Play `alpha` (closed testing) track as a draft, not directly to the public production track.

### Path filters

- Markdown-only changes inside an app directory do not run that app pipeline.
- Root `README.md` and other unrelated documentation do not run application pipelines.
- A workflow file can validate itself on a matching PR, but workflow-only pushes do not deploy applications.
- Web workflows additionally watch root `package.json` and `package-lock.json` because the web app is an npm workspace.
- Manual `workflow_dispatch` remains available where declared.

## DEV AWS lifecycle control

[`DEV Environment Control`](.github/workflows/dev-environment-control.yml) can run `status`, `start`, or `stop` manually. A nightly `07:23 UTC` schedule stops DEV again, including after AWS automatically restarts a stopped RDS instance seven days later.

The implementation is deliberately DEV-only and refuses to operate unless all of these exact targets match:

- AWS account `160885294528`
- region `us-east-1`
- ECS cluster `sanctuary-dev`
- ECS service `sanctuary-api-dev`
- RDS instance `sanctuary-dev-db`

`stop` scales ECS to zero, waits for zero running/pending tasks, then stops RDS. `start` waits for RDS, scales ECS to one, waits for a stable task, and verifies `https://dev-api.mydailysanctuary.com/health`. S3/CloudFront and the deployed ECS/RDS definitions remain in place; this is runtime suspension, not infrastructure deletion.

## Deployment invariants

- Production database credentials come directly from the RDS-managed Secrets Manager secret; do not substitute an SSM password copy.
- API images are smoke-tested before push and ECS rollout.
- Flyway owns schema evolution; do not mutate production schema manually as a normal release step.
- Web upload targets and CloudFront distributions come from GitHub environment configuration.
- Native release signing and store credentials stay in GitHub environment secrets/variables, never in the repository.
- The Android production package name is `com.pamisu.sanctuary`; DEV/UAT suffixes cannot update that Play listing.
- Merging to `main` can upload new native artifacts only when the native app path changed; documentation-only merges do not.

## Maintained operational references

- [Local development](docs/architecture/local-development.md)
- [Deployment and pipelines](docs/architecture/deployment-and-pipelines.md)
- [API production deployment setup](docs/deployment/api-prod-deploy-setup.md)
- [RDS production bootstrap](docs/deployment/rds-production-bootstrap.md)
- [Cognito setup](docs/deployment/cognito-auth-setup.md)
- [Android Play Console setup](docs/android-play-console-setup.md)
- [Android rollout plan](docs/android-rollout-plan.md)
- [iOS App Store verification checklist](docs/deployment/ios-app-store-verification-checklist.md)

Historical architecture and migration files remain useful records, but they are not a substitute for checking current code and workflows before an operational change.
