# Sanctuary Platform

Sanctuary Platform is the long-term web and backend foundation for **Sanctuary**, a Catholic prayer companion centered around daily readings, saints, novenas, prayers, and account-based progress.

This repository is where the modern platform lives:

- a responsive **Angular** web app
- a **Spring Boot** API
- a **PostgreSQL** data model
- a production path for **AWS** deployment
- a backend designed to support **web now** and **mobile clients later**

The legacy native iOS app continues to live separately in:

- [`/Users/pms/repos/Sanctuary`](/Users/pms/repos/Sanctuary)

## Why This Project Exists

Sanctuary started with content and experience patterns proven in the iOS app. This repo is the platform reset that turns those ideas into a scalable architecture:

- public content browsing for saints, novenas, prayers, and liturgical calendar data
- authenticated user features like favorites, account preferences, and novena progress
- one backend that can serve multiple clients over time
- a cleaner engineering foundation than a one-off prototype or static-content shell

In short: this is the product becoming a real platform.

## What’s In This Repo

### `apps/web`

The Angular frontend for:

- home experience
- liturgical calendar
- saints calendar and list views
- novenas calendar, list, and intentions flows
- prayers
- account/profile screens
- custom Sanctuary-styled login, registration, and confirmation flows

### `apps/api`

The Java backend for:

- content APIs
- liturgical calendar APIs
- user profile/favorites/progress APIs
- Cognito-backed authentication flows
- Flyway-managed schema migrations

### `docs`

Architecture, deployment, local development, schema, and production setup notes.

## Current Product Shape

Sanctuary Platform currently supports:

- liturgical day, week, and month browsing
- saints by date and searchable saint lists
- novenas by date, list, and intention search
- prayers and devotional content
- responsive layouts for desktop and mobile browsers
- Cognito-backed account creation and login
- synced user profile data
- favorites and novena progress foundations

## Tech Stack

### Frontend

- Angular 21 (`@angular/core` `^21.2.0`, CLI/build `^21.2.7`)
- TypeScript
- SCSS

### Backend

- Java 21
- Spring Boot
- Maven
- Flyway

### Data

- PostgreSQL

### Auth

- Amazon Cognito

### Infrastructure

- AWS S3 + CloudFront for web delivery
- AWS ECS / ECR for API deployment
- AWS RDS for PostgreSQL
- GitHub Actions for CI/CD

## Repo Layout

```text
sanctuary-platform/
├── apps/
│   ├── api/        # Spring Boot API
│   └── web/        # Angular frontend
├── docs/
│   ├── architecture/
│   └── deployment/
├── backups/        # Local database backups
├── docker-compose.yml
└── README.md
```

## Architecture Notes

The major design choices in this repo are intentional:

- **Monorepo** for the platform, so web and API evolve together
- **Separate legacy iOS repo**, so the new platform can grow without tangling release histories
- **PostgreSQL as source of truth**, because Sanctuary content and user data are relational
- **Cognito for identity**, while Sanctuary owns the long-term application user/profile model
- **Flyway for schema management**, so production evolution is repeatable and auditable

Helpful docs:

- [`/Users/pms/repos/sanctuary-platform/docs/architecture/platform-reset-architecture.md`](/Users/pms/repos/sanctuary-platform/docs/architecture/platform-reset-architecture.md)
- [`/Users/pms/repos/sanctuary-platform/docs/architecture/local-development.md`](/Users/pms/repos/sanctuary-platform/docs/architecture/local-development.md)
- [`/Users/pms/repos/sanctuary-platform/docs/deployment/cognito-auth-setup.md`](/Users/pms/repos/sanctuary-platform/docs/deployment/cognito-auth-setup.md)
- [`/Users/pms/repos/sanctuary-platform/docs/deployment/auth-cognito-progress-2026-04-21.md`](/Users/pms/repos/sanctuary-platform/docs/deployment/auth-cognito-progress-2026-04-21.md)

## Local Development

### Prerequisites

- Node.js / npm
- Java 21
- Docker

### 1. Start PostgreSQL

From the repo root:

```bash
docker compose up -d postgres
```

### 2. Run the API

From:

```bash
cd /Users/pms/repos/sanctuary-platform/apps/api
```

Run:

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/opt/openjdk@21/bin:$PATH mvn spring-boot:run
```

Or use the helper:

```bash
cd /Users/pms/repos/sanctuary-platform
./apps/api/scripts/run-local.sh
```

### 3. Run the Web App

From the repo root:

```bash
npm start --workspace web
```

### Local URLs

- Web: [http://localhost:4200](http://localhost:4200)
- API: [http://localhost:8080](http://localhost:8080)
- Health check: [http://localhost:8080/health](http://localhost:8080/health)

### Recommended Local Workflow

Run in this order:

1. PostgreSQL
2. API
3. Web

## Build & Verification

### Web

```bash
npm run build --workspace web
```

### API tests

```bash
cd /Users/pms/repos/sanctuary-platform/apps/api
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/opt/openjdk@21/bin:$PATH mvn -q test
```

## Authentication Model

Sanctuary now uses a hybrid auth model:

- **Cognito** handles identity and credentials
- **Sanctuary API** handles product-specific auth flows and account linkage
- **PostgreSQL** stores long-term application user data

That gives us room for:

- real user profiles
- synced favorites
- novena progress
- preferences
- streaks and activity
- future subscription and reminder settings

## Deployment Model

### Web

- build Angular app
- publish to S3
- invalidate CloudFront

### API

- build and test Spring Boot app
- build Docker image
- push to ECR
- deploy to ECS
- run Flyway on application startup

### Database

- PostgreSQL on RDS
- schema managed by Flyway
- production content loaded separately from schema migrations

## Branch / Promotion Flow

This repo uses a promotion model:

- feature branch from `dev`
- `feature -> dev`
- `dev -> uat`
- `uat -> prod`
- `prod -> main`

CI validates pull requests through the promotion path, and production deploys are triggered from `main`.

## Why Recruiters / Engineers Should Care

This is not just a UI experiment. It shows end-to-end product engineering:

- monorepo organization
- responsive frontend work
- backend/domain modeling
- database migrations
- auth integration
- cloud deployment
- CI/CD workflow design
- production debugging and operational fixes

It is a practical example of taking a content-heavy consumer app and rebuilding it with a maintainable platform architecture.

## Related Docs Worth Reading

- [`/Users/pms/repos/sanctuary-platform/docs/architecture/deployment-and-pipelines.md`](/Users/pms/repos/sanctuary-platform/docs/architecture/deployment-and-pipelines.md)
- [`/Users/pms/repos/sanctuary-platform/docs/architecture/postgres-schema.md`](/Users/pms/repos/sanctuary-platform/docs/architecture/postgres-schema.md)
- [`/Users/pms/repos/sanctuary-platform/docs/architecture/user-progress-audit-and-schema.md`](/Users/pms/repos/sanctuary-platform/docs/architecture/user-progress-audit-and-schema.md)
- [`/Users/pms/repos/sanctuary-platform/docs/deployment/api-prod-deploy-setup.md`](/Users/pms/repos/sanctuary-platform/docs/deployment/api-prod-deploy-setup.md)
- [`/Users/pms/repos/sanctuary-platform/docs/deployment/rds-production-bootstrap.md`](/Users/pms/repos/sanctuary-platform/docs/deployment/rds-production-bootstrap.md)

## Status

Sanctuary Platform is actively being built out, with the strongest current areas being:

- Angular web experience
- Spring Boot API foundation
- PostgreSQL/Flyway data model
- Cognito-backed auth flow
- AWS deployment path for production

The direction is clear: one serious backend, one serious web client, and a foundation that can support future Android and deeper iOS integration without rebuilding everything again.
