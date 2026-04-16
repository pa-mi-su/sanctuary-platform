# Sanctuary Implementation Phases

## Purpose

Build Sanctuary slowly, clearly, and in documented phases so the platform foundation is correct before deeper implementation work begins.

This document is the execution companion to:

- `platform-reset-architecture.md`
- `postgres-schema.md`
- `java-backend-plan.md`

## Phase 0: Reset and Alignment

Goal:

- remove architectural ambiguity
- preserve the Angular frontend
- lock the new backend direction

Done in this phase:

- keep `apps/web`
- remove DynamoDB and Node backend prototype direction
- document Java + PostgreSQL architecture
- keep legacy iOS app separate in `/Users/pms/repos/Sanctuary`

Exit criteria:

- repo clearly reflects Angular + Java + PostgreSQL direction
- reset is committed as a checkpoint

## Phase 1: Backend Foundation

Goal:

- create the Java backend project without product-specific complexity yet

Planned work:

- create `apps/api` as a Spring Boot application
- decide Java version
- decide build tool:
  - Maven or Gradle
- add base package structure
- add health endpoint
- add environment config
- choose schema migration tool:
  - recommended: Flyway
- define initial database connection strategy

Exit criteria:

- backend boots locally
- app can connect to PostgreSQL
- migrations can run

## Phase 2: Database Foundation

Goal:

- make PostgreSQL the real system of record

Planned work:

- create the initial schema migrations
- add core content tables:
  - saints
  - novenas
  - novena_days
  - prayers
  - liturgical_days
- add foundational user tables:
  - users
  - favorites
  - progress
  - preferences

Exit criteria:

- local database schema can be created from scratch
- schema is reproducible through migrations

## Phase 3: Content Import Pipeline

Goal:

- move real content from the legacy iOS app into PostgreSQL

Planned work:

- inspect legacy JSON carefully
- write importers against PostgreSQL schema
- import saints
- import novenas
- import novena days
- import prayers
- import liturgical content

Important rule:

- legacy app remains source material only
- new backend owns the canonical data after import

Exit criteria:

- PostgreSQL contains real Sanctuary content
- import can be rerun safely

## Phase 4: Public Content API

Goal:

- expose stable read APIs for the frontend

Planned work:

- saints list/detail endpoints
- novenas list/detail/day endpoints
- prayers endpoints
- calendar endpoints

Exit criteria:

- Angular frontend can begin consuming real backend data

## Phase 5: Authentication and User State

Goal:

- support synced favorites and novena progress across clients

Planned work:

- choose and integrate auth provider
  - expected: Cognito
- create user linkage in backend
- add favorites endpoints
- add novena progress endpoints
- add preferences endpoints

Exit criteria:

- authenticated user state works independently of the frontend

## Phase 6: Angular Integration

Goal:

- connect the Angular app to the real backend

Planned work:

- replace stub content with API-backed data
- connect public content screens first
- connect auth-gated features later

Exit criteria:

- Angular app reads real content from Java backend

## Phase 7: Mobile Expansion

Goal:

- support future Android and migration of the native iOS app

Planned work:

- build Android against same backend
- migrate current native iOS app to backend endpoints in phases

Exit criteria:

- one backend serves all clients

## Working Rules

- document first
- scaffold second
- import real data before building too much client logic
- do not mix legacy app architecture into the new platform
- preserve a clean checkpoint at the end of each phase
