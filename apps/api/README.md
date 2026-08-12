# Sanctuary API

`apps/api` is the authoritative backend for Sanctuary's web, iOS, and Android clients. It owns liturgical calculations, API-served content, Cognito account operations, Sanctuary profile/state persistence, and Flyway schema evolution.

## Stack and boundaries

- Java 21 exactly (`maven-enforcer-plugin` requires `[21,22)`)
- Spring Boot 3.5.6
- Spring MVC, Validation, Security OAuth2 Resource Server, Actuator, and JDBC
- Flyway with PostgreSQL support
- AWS SDK Cognito Identity Provider client
- Maven and a two-stage Java 21 Docker image

The API is the only application layer that accesses PostgreSQL. Clients never connect to RDS, Cognito, or Secrets Manager directly.

## Source layout

```text
src/main/java/app/sanctuary/api/
├── auth/       # Cognito flows, native/web sessions, validation
├── calendar/   # anchors, liturgical days, novena serving windows
├── config/     # security, CORS, cookies, abuse protection
├── content/    # saint, prayer, novena, intention, patronage repositories/API
├── health/     # GET /health
└── user/       # account, preferences, favorites, commitments, deletion

src/main/resources/
├── application.yml
├── application-{local,dev,uat,prod}.yml
└── db/migration/   # V1 through V15
```

Each feature follows controller -> service/repository -> JDBC/database boundaries. Liturgical calculation is service code; localized devotional content and serving rules are database-backed.

## Profiles and configuration

| Profile | Database                    | CORS/auth behavior                                                                        |
| ------- | --------------------------- | ----------------------------------------------------------------------------------------- |
| base    | no URL/credentials          | auth disabled by default; abuse protection configured                                     |
| `local` | required environment values | localhost/DEV origins; auth defaults on; local Cognito defaults; non-secure `Lax` cookies |
| `dev`   | required environment values | localhost/DEV origins; auth defaults off unless supplied; secure `None` cookies           |
| `uat`   | required environment values | only database and port overrides; inherits base auth behavior                             |
| `prod`  | required environment values | production web origins; auth defaults off unless supplied; secure `None` cookies          |

Required database variables:

- `SANCTUARY_DB_URL`
- `SANCTUARY_DB_USERNAME`
- `SANCTUARY_DB_PASSWORD`

Auth/runtime variables used by deployed profiles:

- `SANCTUARY_AUTH_ENABLED`
- `SANCTUARY_COGNITO_ISSUER_URI`
- `SANCTUARY_COGNITO_USER_POOL_ID`
- `SANCTUARY_COGNITO_CLIENT_ID`
- `SANCTUARY_AUTH_COOKIE_DOMAIN`
- `SANCTUARY_AUTH_COOKIE_SECURE`
- `SANCTUARY_AUTH_COOKIE_SAME_SITE`
- `SANCTUARY_AUTH_COOKIE_REFRESH_MAX_AGE_DAYS`
- `SANCTUARY_API_PORT` (default `8080`)

Production `SANCTUARY_DB_PASSWORD` is injected from the RDS-managed Secrets Manager secret. It is not sourced from an SSM password copy.

## Security model

When `SANCTUARY_AUTH_ENABLED=true`:

- `/health`, `/actuator/**`, `/auth/**`, `/calendar/**`, and `/content/**` are public.
- `/me/**` requires authentication.
- every other request is denied.
- JWT validation checks the configured Cognito issuer and, when configured, accepts a matching JWT audience or `client_id`.
- bearer credentials can come from the `Authorization` header or the API's ID-token cookie.
- configured CORS origins may send credentials.

CSRF is disabled in the current security chain. Web cookies therefore use controlled origins plus HttpOnly/Secure/SameSite settings; those settings must not be loosened casually.

Auth abuse protection is enabled by default and applies per client IP to native register/login/forgot-password/resend-confirmation paths. Default windows are 5 registrations/hour, 20 logins/15 minutes, 5 forgot-password requests/hour, and 5 confirmation resends/hour. Common dummy domains are blocked for registration. Request-summary logs retain the email domain and Java hash code of the normalized email, not the plaintext email, password, or confirmation code.

## Complete HTTP surface

All dates are ISO `YYYY-MM-DD`. Supported `lang` values are `en`, `es`, and `pl`.

### Health

| Method | Path               | Behavior                           |
| ------ | ------------------ | ---------------------------------- |
| `GET`  | `/health`          | Simple application health response |
| `GET`  | `/actuator/health` | Spring Actuator health             |

### Authentication

| Method | Path                        | Client/use                                 |
| ------ | --------------------------- | ------------------------------------------ |
| `POST` | `/auth/register`            | Create Cognito user and start confirmation |
| `POST` | `/auth/confirm`             | Confirm registration code                  |
| `POST` | `/auth/resend-confirmation` | Resend code                                |
| `POST` | `/auth/login`               | Native token session                       |
| `POST` | `/auth/refresh`             | Native refresh-token exchange              |
| `POST` | `/auth/web/login`           | Web login and HttpOnly cookie session      |
| `POST` | `/auth/web/refresh`         | Refresh web cookies                        |
| `POST` | `/auth/web/logout`          | Clear web cookies                          |
| `POST` | `/auth/forgot-password`     | Send reset code                            |
| `POST` | `/auth/reset-password`      | Native reset and token session             |
| `POST` | `/auth/web/reset-password`  | Web reset and cookie session               |

### Calendar

| Method | Path                                         | Validation                                     |
| ------ | -------------------------------------------- | ---------------------------------------------- |
| `GET`  | `/calendar/day/{date}`                       | One liturgical day                             |
| `GET`  | `/calendar/range?start=&end=`                | Inclusive, ordered range; maximum 62 days      |
| `GET`  | `/calendar/anchors/{year}`                   | Year must be 1900 through 4099                 |
| `GET`  | `/calendar/novenas/{novenaId}/window/{year}` | Same year bounds; `404` without a serving rule |

### Saints

| Method | Path                                      |
| ------ | ----------------------------------------- |
| `GET`  | `/content/saints/search?lang=&query=`     |
| `GET`  | `/content/saints?month=&day=&lang=`       |
| `GET`  | `/content/saints/range?start=&end=&lang=` |
| `GET`  | `/content/saints/{slug}?lang=`            |

### Prayers

| Method | Path                                                       |
| ------ | ---------------------------------------------------------- |
| `GET`  | `/content/prayers?lang=&query=&category=&excludeCategory=` |
| `GET`  | `/content/prayers/{slug}?lang=`                            |

The clients use category filtering to separate rosaries from the ordinary prayer list.

### Novenas and intentions

| Method | Path                                            |
| ------ | ----------------------------------------------- |
| `GET`  | `/content/novenas?lang=&query=`                 |
| `GET`  | `/content/novenas/intentions?lang=&query=`      |
| `GET`  | `/content/novenas/calendar?start=&end=&lang=`   |
| `GET`  | `/content/novenas/{slug}?lang=`                 |
| `GET`  | `/content/intentions/search?lang=&query=`       |
| `GET`  | `/content/intentions/terms?lang=&query=`        |
| `GET`  | `/content/intentions/terms/{key}/novenas?lang=` |

The term routes use normalized global intention terms/aliases; `/content/novenas/intentions` remains a direct novena-intention search route.

### Patronages

| Method | Path                                           |
| ------ | ---------------------------------------------- |
| `GET`  | `/content/patronages/terms?lang=&query=`       |
| `GET`  | `/content/patronages/terms/{key}/saints?lang=` |

### Authenticated user state

| Method   | Path                                | Behavior                                                                  |
| -------- | ----------------------------------- | ------------------------------------------------------------------------- |
| `GET`    | `/me`                               | Upsert/map Cognito identity and return profile/counts/preferences         |
| `DELETE` | `/me`                               | Delete Cognito user and owned Sanctuary data; retain deletion marker/hash |
| `PUT`    | `/me/preferences`                   | Update language, time zone, reminders, email, onboarding                  |
| `GET`    | `/me/favorites`                     | List favorites                                                            |
| `PUT`    | `/me/favorites/{itemType}/{itemId}` | Add saint/novena/prayer favorite                                          |
| `DELETE` | `/me/favorites/{itemType}/{itemId}` | Remove favorite                                                           |
| `GET`    | `/me/novena-commitments`            | List progress                                                             |
| `PUT`    | `/me/novena-commitments/{novenaId}` | Create/update progress and reminder config                                |
| `DELETE` | `/me/novena-commitments/{novenaId}` | Remove commitment                                                         |

## PostgreSQL and Flyway

Migrations `V1`-`V15` define:

- `platform_metadata`
- `saints`, `saint_tags`, `saint_patronages`, `saint_sources`
- `prayers`, `prayer_tags`
- `novenas`, `novena_tags`, `novena_days`, `novena_intentions`, `novena_serving_rules`
- `users`, `user_preferences`, `user_favorites`, `user_novena_commitments`, `user_activity_events`
- `deleted_user_accounts`
- `content_intentions`, `content_intention_aliases`, `novena_intention_links`, `saint_intention_links`
- `content_patronages`, `content_patronage_aliases`, `saint_patronage_links`

Flyway runs at application startup. Legacy content is not imported at startup. Import scripts under the repository root are explicit operational tools; inspect their arguments and target environment before running them.

Account deletion first deletes the Cognito identity, records the Cognito subject and SHA-256 normalized email hash in `deleted_user_accounts`, and deletes the local user. Foreign keys/cascade and explicit cleanup remove owned preferences, favorites, commitments, and activity.

## Local development

At repository root, create a gitignored `.env`, provide the database values, and run:

```bash
docker compose up -d postgres
./apps/api/scripts/run-local.sh
```

The script locates Java 21, exports root `.env`, verifies the three database variables, and runs `spring-boot:run` with `local`.

Direct commands:

```bash
cd apps/api
mvn -q test
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Local URLs:

- `http://localhost:8080/health`
- `http://localhost:8080/actuator/health`

## Tests

```bash
cd apps/api
mvn -q test
```

The test suite covers Cognito flows, security/CORS and abuse configuration, supported languages/content validation, saint repository behavior, liturgical calculations and novena serving rules, plus user account/profile repositories and services.

## Docker and deployment

`Dockerfile` builds with Maven 3.9.9/Temurin 21, skips tests during the image build because CI runs them first, and executes the packaged JAR on a Temurin 21 JRE.

- DEV: `.github/workflows/api-dev-deploy.yml` validates PRs to `dev`; a qualifying push to `dev` tests, builds, smoke-tests, pushes ECR, updates `sanctuary-api-dev` ECS, and verifies rollout.
- Production: `.github/workflows/api-prod-deploy.yml` validates PRs across promotion branches; a qualifying push to `main` tests, builds, smoke-tests, pushes ECR, updates production ECS, and verifies both rollout and public health.
- No UAT API deployment job exists. Native UAT clients use the production API.

Markdown-only changes under `apps/api` do not run either API pipeline. Workflow-only changes can validate on PR but do not deploy on push.

DEV runtime can be suspended with `.github/workflows/dev-environment-control.yml`; see the root README for exact targets and sequencing.
