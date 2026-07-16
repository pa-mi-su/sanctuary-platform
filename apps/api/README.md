# Sanctuary API

`apps/api` is the Java backend for Sanctuary. It is the shared API used by the Angular web app, the iOS app, and the Android app.

The API owns:

- liturgical calendar calculation
- saints, prayers, novenas, and date-based content lookup
- novena serving windows
- Cognito-backed auth flows
- Sanctuary user profile and preferences
- favorites
- novena commitments/progress
- Flyway-managed PostgreSQL schema changes

## Stack

- Java 21
- Spring Boot 3.5
- Spring MVC
- Spring Security OAuth2 Resource Server
- Spring JDBC
- Flyway
- PostgreSQL
- AWS SDK for Cognito
- Maven
- Docker

## Structure

```text
apps/api/
├── src/main/java/app/sanctuary/api/
│   ├── auth/       # Cognito-backed auth controllers/services
│   ├── calendar/   # Liturgical calendar and novena serving rules
│   ├── config/     # Security, auth, and web config
│   ├── content/    # Saints, prayers, novenas
│   ├── health/     # /health
│   └── user/       # /me, favorites, preferences, novena commitments
├── src/main/resources/
│   ├── application*.yml
│   └── db/migration/
├── scripts/
├── Dockerfile
├── pom.xml
└── README.md
```

## Runtime Profiles

The API uses explicit Spring profiles:

- `local`
- `dev`
- `uat`
- `prod`

Base properties live in [`src/main/resources/application.yml`](src/main/resources/application.yml).

Environment-specific properties live in:

- [`src/main/resources/application-local.yml`](src/main/resources/application-local.yml)
- [`src/main/resources/application-dev.yml`](src/main/resources/application-dev.yml)
- [`src/main/resources/application-uat.yml`](src/main/resources/application-uat.yml)
- [`src/main/resources/application-prod.yml`](src/main/resources/application-prod.yml)

Expected runtime configuration includes:

- `SANCTUARY_DB_URL`
- `SANCTUARY_DB_USERNAME`
- `SANCTUARY_DB_PASSWORD`
- `SANCTUARY_API_PORT` (optional; defaults to `8080`)
- `SANCTUARY_AUTH_ENABLED`
- `SANCTUARY_COGNITO_ISSUER_URI`
- `SANCTUARY_COGNITO_CLIENT_ID`
- `SANCTUARY_COGNITO_USER_POOL_ID`
- `SANCTUARY_AUTH_COOKIE_DOMAIN` (Dev/Prod)
- `SANCTUARY_AUTH_COOKIE_SECURE`
- `SANCTUARY_AUTH_COOKIE_SAME_SITE`
- `SANCTUARY_AUTH_COOKIE_REFRESH_MAX_AGE_DAYS`

The base profile disables auth. Local enables it by default with development-friendly cookie settings; Dev and Prod require explicit auth enablement and Cognito values. The UAT profile currently supplies only database and port settings and therefore inherits the base auth-disabled behavior.

In production, `SANCTUARY_DB_PASSWORD` must come directly from the RDS-managed AWS Secrets Manager secret. Do not use an SSM copy for prod DB credentials.

## Local Development

From the repo root, start PostgreSQL:

```bash
docker compose up -d postgres
```

Run the API with the local helper:

```bash
./apps/api/scripts/run-local.sh
```

Or run Maven directly:

```bash
cd apps/api
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/opt/openjdk@21/bin:$PATH mvn spring-boot:run
```

Local URLs:

- API: `http://localhost:8080`
- Health: `http://localhost:8080/health`
- Actuator health: `http://localhost:8080/actuator/health`

## Tests

```bash
cd apps/api
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/opt/openjdk@21/bin:$PATH mvn -q test
```

The Maven enforcer requires Java 21.

The suite covers calendar, auth, validation, CORS, and user service/repository behavior. The novena rule audit uses `SANCTUARY_TEST_DATABASE_URL` when a populated database is reachable and skips by assumption otherwise.

## API Surface

### Health

- `GET /health`
- `GET /actuator/health`

### Auth

- `POST /auth/register`
- `POST /auth/confirm`
- `POST /auth/resend-confirmation`
- `POST /auth/login`
- `POST /auth/refresh`
- `POST /auth/forgot-password`
- `POST /auth/reset-password`
- `POST /auth/web/login`
- `POST /auth/web/refresh`
- `POST /auth/web/logout`
- `POST /auth/web/reset-password`

### Calendar

- `GET /calendar/day/{date}`
- `GET /calendar/range?start=&end=`
- `GET /calendar/anchors/{year}`
- `GET /calendar/novenas/{novenaId}/window/{year}`

`/calendar/range` and the content calendar/range endpoints accept at most 62 inclusive days. Calculated anchor/window years are limited to 1900–4099. Content language values are `en`, `es`, and `pl`.

### Saints

- `GET /content/saints?month=&day=&lang=`
- `GET /content/saints/range?start=&end=&lang=`
- `GET /content/saints/search?query=&lang=`
- `GET /content/saints/{slug}?lang=`

### Prayers

- `GET /content/prayers?query=&lang=&category=&excludeCategory=`
- `GET /content/prayers/{slug}?lang=`

### Novenas

- `GET /content/novenas?query=&lang=`
- `GET /content/novenas/intentions?query=&lang=`
- `GET /content/novenas/calendar?start=&end=&lang=`
- `GET /content/novenas/{slug}?lang=`

### Intentions And Patronages

- `GET /content/intentions/search?query=&lang=`
- `GET /content/intentions/terms?query=&lang=`
- `GET /content/intentions/terms/{key}/novenas?lang=`
- `GET /content/patronages/terms?query=&lang=`
- `GET /content/patronages/terms/{key}/saints?lang=`

### User State

These require authentication:

Native clients send a bearer token. Browser requests may instead authenticate with the scoped `HttpOnly` cookie created by `/auth/web/login`.

- `GET /me`
- `DELETE /me`
- `PUT /me/preferences`
- `GET /me/favorites`
- `PUT /me/favorites/{itemType}/{itemId}`
- `DELETE /me/favorites/{itemType}/{itemId}`
- `GET /me/novena-commitments`
- `PUT /me/novena-commitments/{novenaId}`
- `DELETE /me/novena-commitments/{novenaId}`

Favorite item types are `saint`, `novena`, and `prayer`; commitment statuses are `active`, `paused`, and `completed`.

When auth is enabled, health, actuator, auth, calendar, and content routes are public. `/me/**` accepts either a native bearer token or the scoped browser ID-token cookie; all otherwise-unmatched routes are denied. In-memory abuse limits cover registration, native login, forgot-password, and confirmation-code resend—not every `/auth/**` route.

## Database And Content

Flyway migrations live in [`src/main/resources/db/migration`](src/main/resources/db/migration).

The API should not import legacy JSON content during app startup. Content bootstrap/import work should be explicit and operational.

Current external import tool:

- [`../../scripts/import_novenas.py`](../../scripts/import_novenas.py)

Run from the platform repo root:

```bash
python3 scripts/import_novenas.py
```

## Deployment

The production workflow is [`../../.github/workflows/api-prod-deploy.yml`](../../.github/workflows/api-prod-deploy.yml).

Production flow:

1. run Maven tests
2. build the Docker image
3. push to ECR
4. update ECS
5. start with `prod` profile
6. run Flyway validation/migrations
7. pass `/health`

Related docs:

- [`../../docs/deployment/api-prod-deploy-setup.md`](../../docs/deployment/api-prod-deploy-setup.md)
- [`../../docs/deployment/rds-production-bootstrap.md`](../../docs/deployment/rds-production-bootstrap.md)
- [`../../docs/architecture/postgres-schema.md`](../../docs/architecture/postgres-schema.md)
- [`../../docs/architecture/liturgical-engine-plan.md`](../../docs/architecture/liturgical-engine-plan.md)
