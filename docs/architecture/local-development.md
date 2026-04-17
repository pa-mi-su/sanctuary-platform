# Sanctuary Local Development

## Goal

Use Docker for local infrastructure, starting with PostgreSQL.

Do not introduce Kubernetes at this stage.

## Decision

### Use Docker

Reasons:

- repeatable local development
- easier onboarding
- isolated PostgreSQL setup
- lower machine-specific drift

### Do not use Kubernetes yet

Reasons:

- too much operational complexity for the current stage
- slows down schema and API iteration
- not necessary to validate the product architecture

## Local Stack

### Required now

- Angular frontend in `apps/web`
- Java backend in `apps/api`
- PostgreSQL in Docker

### Optional later

- backend containerization
- local admin tools
- search infrastructure

## Local Ports

- Angular dev server:
  - `4200`
- Java backend:
  - `8080`
- PostgreSQL:
  - `5432`

## Local Environment Variables

Use an untracked local `.env` file at the repo root.

Commit only `.env.example`.

Expected variables:

- `POSTGRES_DB`
- `POSTGRES_USER`
- `POSTGRES_PASSWORD`
- `SANCTUARY_DB_URL`
- `SANCTUARY_DB_USERNAME`
- `SANCTUARY_DB_PASSWORD`
- `SANCTUARY_API_PORT`

Real secrets must not be committed.

Important note:

- Docker Compose reads the root `.env`
- Spring Boot configuration structure lives in `application.yml`
- Spring Boot uses explicit profile files:
  - `application-local.yml`
  - `application-dev.yml`
  - `application-uat.yml`
  - `application-prod.yml`
- use `apps/api/scripts/run-local.sh` to load `.env` into the process environment and start the app with the `local` profile

Java standard:

- local API development should use Java 21
- Maven is configured to fail fast if another major Java version is used

## Working Rule

Local infrastructure should be simple enough that:

- a new machine can start the database in one command
- the backend can connect without manual database setup
- frontend and backend work can proceed independently
## Database Persistence

Local PostgreSQL data is stored in the named Docker volume `sanctuary_postgres_data`.

Important behavior:

- `docker compose stop` keeps the data
- `docker compose down` keeps the data
- `docker compose down -v` deletes the volume and removes the local database contents

Because Sanctuary now contains imported legacy content, the local workflow should not rely on the Docker volume alone.

## Database Backups

Use the project backup scripts from the repo root:

```bash
bash scripts/export_db.sh
```

This creates:

- `backups/sanctuary-YYYYMMDD-HHMMSS.dump`
- `backups/sanctuary-YYYYMMDD-HHMMSS.sql`
- `backups/sanctuary_latest.dump`
- `backups/sanctuary_latest.sql`

Restore the latest backup:

```bash
bash scripts/restore_db.sh
```

Restore a specific backup:

```bash
bash scripts/restore_db.sh backups/sanctuary-YYYYMMDD-HHMMSS.dump
```

The restore script resets the `public` schema before loading the backup, so treat it as a full local database replacement.
