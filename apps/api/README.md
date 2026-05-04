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
- `SANCTUARY_AUTH_ENABLED`
- `SANCTUARY_COGNITO_ISSUER_URI`
- `SANCTUARY_COGNITO_CLIENT_ID`

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

### Calendar

- `GET /calendar/day/{date}`
- `GET /calendar/range?start=&end=`
- `GET /calendar/anchors/{year}`
- `GET /calendar/novenas/{novenaId}/window/{year}`

### Saints

- `GET /content/saints?month=&day=&lang=`
- `GET /content/saints/range?start=&end=&lang=`
- `GET /content/saints/search?query=&lang=`
- `GET /content/saints/{slug}?lang=`

### Prayers

- `GET /content/prayers?query=&lang=`
- `GET /content/prayers/{slug}?lang=`

### Novenas

- `GET /content/novenas?query=&lang=`
- `GET /content/novenas/intentions?query=&lang=`
- `GET /content/novenas/calendar?start=&end=&lang=`
- `GET /content/novenas/{slug}?lang=`

### User State

These require a bearer token:

- `GET /me`
- `PUT /me/preferences`
- `GET /me/favorites`
- `PUT /me/favorites/{itemType}/{itemId}`
- `DELETE /me/favorites/{itemType}/{itemId}`
- `GET /me/novena-commitments`
- `PUT /me/novena-commitments/{novenaId}`
- `DELETE /me/novena-commitments/{novenaId}`

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
