# RDS Production Bootstrap

This guide covers the production PostgreSQL setup and the first controlled content load.

## Goal

Provision a real production database that the Sanctuary API can use safely.

## Core Rule

- Flyway handles schema
- bootstrap handles initial content
- app startup never imports legacy content

## Step 1: Create Production RDS PostgreSQL

Recommended initial settings:

- engine: PostgreSQL
- version: align with local baseline where practical
- instance class: start modestly, then scale with load
- storage: gp3
- automated backups: enabled
- deletion protection: enabled
- public access: disabled

## Step 2: Network And Access

- allow inbound database access only from the API runtime
- do not allow ad hoc public access from the internet

## Step 3: Create Production Database Credentials

Store these outside the repo:

- database URL
- username
- password

Production credential rule:

- use the RDS-managed AWS Secrets Manager secret as the only production DB password source
- inject `SANCTUARY_DB_PASSWORD` from the secret's `password` JSON field
- do not create or use an SSM copy such as `/sanctuary/prod/db/password`

Keep automatic RDS secret rotation disabled until production has automation that force-redeploys the API after every rotation.

Manual rotation runbook:

1. rotate the RDS-managed database secret/password
2. force a production API deployment
3. verify `/health`
4. verify API logs show a successful PostgreSQL connection and Flyway validation

## Step 4: Point ECS To RDS

The API service will need:

- `SANCTUARY_DB_URL`
- `SANCTUARY_DB_USERNAME`
- `SANCTUARY_DB_PASSWORD`
- `SPRING_PROFILES_ACTIVE=prod`

## Step 5: Run Flyway

Flyway should create:

- tables
- indexes
- constraints

This must happen before any content bootstrap.

## Step 6: Run Initial Content Bootstrap

Initial production content load should include:

- saints
- prayers
- novenas
- child rows such as:
  - saint sources
  - prayer tags
  - novena days
  - novena intentions
  - novena serving rules

This should be a controlled, one-time bootstrap step.

## Step 7: Verify Production Content

Verify at minimum:

- saints count
- prayers count
- novenas count
- child-table counts
- sample endpoint reads from the live API

## Step 8: Snapshot After Bootstrap

After schema and content are verified:

- take an RDS snapshot

This becomes the first clean production restore point.

## Required Future Decision

Before we execute the bootstrap in production, we need to choose one of these:

1. run explicit SQL/bootstrap scripts against RDS
2. restore a vetted production seed dump

Recommended:

- explicit bootstrap scripts

Why:

- clearer audit trail
- easier to reason about than a monolithic dump
- safer to document and repeat in lower environments
