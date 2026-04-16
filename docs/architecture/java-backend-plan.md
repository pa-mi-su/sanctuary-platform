# Sanctuary Java Backend Plan

## Goal

Build the Sanctuary backend as a Java service that serves:

- Angular web
- future Android
- current native iOS app

Recommended framework:

- Spring Boot

## Why Spring Boot

- mature PostgreSQL support
- strong REST API tooling
- strong validation and security support
- easy future layering for auth and admin
- appropriate for a long-term product backend

## Recommended Application Modules

### content

Responsibilities:

- saints APIs
- novenas APIs
- novena day APIs
- prayers APIs
- liturgical/calendar APIs

### user

Responsibilities:

- user profile
- favorites
- novena progress
- preferences

### auth

Responsibilities:

- JWT verification
- Cognito integration
- route protection
- user identity mapping

### import

Responsibilities:

- import legacy JSON from `/Users/pms/repos/Sanctuary`
- normalize and load Postgres content
- make migration rerunnable

### search

Phase 1:

- keep search simple

Phase 2:

- add dedicated search indexing path

## Suggested Package Layout

- `app.sanctuary.api.config`
- `app.sanctuary.api.content`
- `app.sanctuary.api.user`
- `app.sanctuary.api.auth`
- `app.sanctuary.api.importer`
- `app.sanctuary.api.common`

## Initial REST Shape

### Public routes

- `GET /content/saints`
- `GET /content/saints/{slug}`
- `GET /content/novenas`
- `GET /content/novenas/{slug}`
- `GET /content/novenas/{slug}/days/{dayNumber}`
- `GET /content/prayers`
- `GET /calendar/day/{yyyy-mm-dd}`

### Authenticated routes

- `GET /me`
- `GET /me/favorites`
- `PUT /me/favorites/saints/{slug}`
- `PUT /me/favorites/novenas/{slug}`
- `DELETE /me/favorites/saints/{slug}`
- `DELETE /me/favorites/novenas/{slug}`
- `GET /me/novena-progress`
- `PUT /me/novena-progress/{slug}`
- `PUT /me/preferences`

## Migration Strategy

### Phase 1

- scaffold Spring Boot app
- connect PostgreSQL
- create schema
- import saints and novenas from legacy JSON
- expose public read APIs

### Phase 2

- add Cognito integration
- create user tables
- expose favorites and progress APIs

### Phase 3

- connect Angular frontend
- connect native iOS app gradually
- build Android against the same APIs

## Repo Decision

Keep a single new platform monorepo.

Why:

- frontend and backend contracts evolve together
- docs stay in one place
- easier coordinated schema and API changes
- lower coordination overhead than separate repos

## Immediate Next Step

Create a clean Java backend scaffold in `apps/api` and keep `apps/web` unchanged.
