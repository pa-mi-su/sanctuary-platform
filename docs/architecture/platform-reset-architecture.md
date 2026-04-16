# Sanctuary Platform Reset Architecture

## Goal

Build Sanctuary as a long-term platform with:

- Angular frontend
- Java backend
- PostgreSQL primary database
- authenticated user accounts
- one backend that can serve:
  - web
  - future Android
  - current native iOS app

This document replaces the earlier DynamoDB-first direction as the recommended target architecture.

## Repositories

### Keep separate

- Legacy native iOS app:
  - `/Users/pms/repos/Sanctuary`
- New platform:
  - `/Users/pms/repos/sanctuary-platform`

The legacy iOS app remains the source of current production content and a maintenance app.
The new platform becomes the long-term system.

### Within the new platform

Use one monorepo.

Recommended structure:

- `apps/web`
  - Angular frontend
- `apps/api`
  - Java backend
- `packages/shared`
  - shared API contracts, schemas, and generated types where useful
- `docs/architecture`
  - system design and decisions
- later:
  - `infra`
    - deployment/infrastructure definitions

## What We Keep

- Keep the Angular work in `apps/web`
- Keep the monorepo
- Keep architecture docs that still help explain the product direction
- Keep importer learnings from the DynamoDB prototype as reference material only

## What Changes

- DynamoDB is no longer the target primary database
- Node API/import prototype work is no longer the final backend direction
- PostgreSQL becomes the system of record for:
  - content
  - user state
  - relationships
  - future editorial/admin needs

## Recommended Stack

### Frontend

- Angular
- keep current web shell and UI work

### Backend

- Java backend
- recommended framework:
  - Spring Boot

Why:

- strong Postgres ecosystem
- mature auth, REST, validation, and data tooling
- good long-term maintainability
- suitable for a serious multi-client backend

### Database

- PostgreSQL

Why:

- better fit for growing content models
- better fit for user state and relational querying
- easier for admin/editor workflows later
- better foundation for search indexing later
- avoids shaping the entire system around DynamoDB access patterns

### Authentication

- Cognito can still be used for identity
- backend should treat auth as required for synced user features
- anonymous browsing should still be allowed for public content

## Core Product Domains

### Public content

- saints
- novenas
- novena days
- novena intentions
- prayers
- liturgical calendar

### Authenticated user state

- favorite saints
- favorite novenas
- novena progress
- completed novena days
- reminder preferences
- language/preferences
- later subscriptions and personalization

## Proposed Backend Modules

### Content module

Responsibilities:

- saints APIs
- novenas APIs
- prayers APIs
- liturgical/calendar APIs

### User module

Responsibilities:

- profile
- favorites
- progress
- preferences

### Auth module

Responsibilities:

- token validation
- account/user linkage
- access control for authenticated routes

### Search module

Not phase 1, but should be planned from the start.

Search targets:

- saint names
- saint patronages
- novena titles
- novena intentions
- prayer titles
- text discovery across summaries and metadata

Recommended approach:

- PostgreSQL as source of truth
- dedicated search layer later if needed

## Database Direction

### Core tables expected

- `saints`
- `novenas`
- `novena_days`
- `prayers`
- `liturgical_days`
- `saint_feast_days` or equivalent date-mapping table
- `users`
- `favorite_saints`
- `favorite_novenas`
- `novena_progress`
- `novena_day_progress`
- `user_preferences`

This is intentionally relational and explicit.

## Migration Strategy

1. Keep legacy iOS app separate
2. Treat legacy JSON/content as migration source material
3. Build Java/Postgres backend in `sanctuary-platform`
4. Load canonical content into Postgres
5. Connect Angular frontend to the new backend
6. Build Android against the same backend
7. Migrate the current iOS app to the backend in phases

## Immediate Next Steps

1. Preserve the Angular app as-is
2. Stop investing further in DynamoDB-first backend work
3. Replace `apps/api` direction with Java backend scaffolding
4. Design the PostgreSQL schema before writing more import code
5. Document repo conventions and domain model before implementation

## Decision

Current recommendation:

- separate legacy app repo: yes
- separate repos for web/backend within the new platform: no
- keep one new platform monorepo: yes
- keep Angular work: yes
- start over on backend/data architecture: yes
- use PostgreSQL as primary database: yes
