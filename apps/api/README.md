# Sanctuary API

Spring Boot backend for the Sanctuary platform.

Current phase:

- Maven build
- Java 21
- PostgreSQL configuration
- Flyway migrations
- basic health endpoint

## Environment profiles

The API now uses explicit Spring profiles:

- `local`
- `dev`
- `uat`
- `prod`

Base property structure lives in `application.yml`.

Environment-specific values live in:

- `application-local.yml`
- `application-dev.yml`
- `application-uat.yml`
- `application-prod.yml`

## Local run

Use the local runner so the repo `.env` is loaded before Spring Boot starts and the `local` profile is active:

```bash
./scripts/run-local.sh
```

## Legacy data migration

Legacy JSON imports are outside the responsibility of the Spring Boot application.

The saints data has already been migrated into PostgreSQL once. Future one-time legacy imports should be handled by explicit migration scripts or tools outside the application runtime so the API codebase stays focused on serving product behavior.

Current external import tools:

- novenas: `/Users/pms/repos/sanctuary-platform/scripts/import_novenas.py`

Run the novena import from the platform repo root:

```bash
python3 scripts/import_novenas.py
```
