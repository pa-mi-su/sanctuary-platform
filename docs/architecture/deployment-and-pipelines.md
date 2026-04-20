# Sanctuary Deployment And Pipelines

## Goal

Launch Sanctuary with:

- a production Angular web app
- a production Java API
- a production PostgreSQL database
- repeatable CI/CD pipelines for `dev`, `uat`, and `prod`

This document is intentionally high level and execution-oriented.

The rule is:

- define the deployment model first
- build one pipeline at a time
- verify each environment before moving to the next one

## Core Deployment Model

### Web

- Angular app in `apps/web`
- build as static assets
- host in `S3`
- serve through `CloudFront`

Why:

- simple
- cheap
- fast global delivery
- already aligned with the current static-site direction

### API

- Java Spring Boot API in `apps/api`
- package as a Docker image
- push image to `ECR`
- run in AWS as a managed container service

Recommended first target:

- `AWS App Runner`

Why:

- simpler than `ECS` for first launch
- enough for the current product stage
- easy HTTPS endpoint and autoscaling baseline

Possible later target:

- `ECS Fargate`

### Database

- PostgreSQL in `RDS`
- separate instances or clusters per environment

Important rule:

- production data must never depend on local Docker volumes or ad hoc dumps

## Environment Model

Use three real environments:

- `dev`
- `uat`
- `prod`

Each environment should have:

- its own web deploy target
- its own API service
- its own database
- its own secrets/config values

Do not share production infrastructure with lower environments.

## Branch To Environment Mapping

Recommended mapping:

- `dev` branch -> `dev` environment
- `uat` branch -> `uat` environment
- `main` branch -> `prod` environment

Production deploys should require approval.

## Pipeline Strategy

We should not build everything at once.

We should build pipelines in this order:

1. web pipeline
2. API pipeline
3. database migration/bootstrap workflow
4. full environment promotion flow

## Pipeline 1: Angular Web

### Purpose

Build and deploy the Angular SPA safely and repeatably.

### Steps

- install dependencies
- run build
- publish `dist/web` to the environment S3 bucket
- invalidate the matching CloudFront distribution

### First implementation

The first concrete pipeline is:

- GitHub Actions workflow:
  - `.github/workflows/web-prod-deploy.yml`
- trigger on:
  - pushes to `prod-ready-web-shell` for the first live deployment phase
  - manual `workflow_dispatch`
- current build/deploy path:
  - `npm ci`
  - `npm run build --workspace web`
  - sync `apps/web/dist/web/browser` to the production S3 bucket
  - invalidate the production CloudFront distribution

Current decision:

- the existing `mydailysanctuary.com` static site bucket and CloudFront distribution will become the Angular production deploy target
- this intentionally replaces the current static content instead of creating a separate app subdomain first
- because the real launch-ready app currently lives on `prod-ready-web-shell`, the first production web deploy path is temporarily tied to that branch rather than `main`
- after branch alignment is complete, production deploy triggering should move back to `main`

### Required GitHub configuration

For the first production web pipeline, configure:

#### Repository or environment variables

- `AWS_REGION`
- `WEB_PROD_S3_BUCKET`
- `WEB_PROD_CLOUDFRONT_DISTRIBUTION_ID`

#### Secret

- `AWS_DEPLOY_ROLE_ARN`

Recommended approach:

- use GitHub Actions OIDC with an AWS IAM role
- do not use long-lived AWS access keys if we can avoid them

### Environment needs

- S3 bucket per environment
- CloudFront distribution per environment
- environment-specific frontend config

### Success criteria

- SPA loads through CloudFront
- deep links resolve correctly to `index.html`
- support/privacy/about continue to work inside the SPA

## Pipeline 2: Java API

### Purpose

Build, test, package, and deploy the Spring Boot API safely.

### Steps

- run `mvn test`
- build Docker image
- tag image per environment/revision
- push image to `ECR`
- deploy to App Runner
- run smoke checks after deploy

### First implementation

The first concrete API pipeline is:

- GitHub Actions workflow:
  - `.github/workflows/api-prod-deploy.yml`
- current trigger:
  - pushes to `prod-ready-web-shell`
  - manual `workflow_dispatch`
- current runtime target:
  - `AWS App Runner`
- current registry target:
  - `Amazon ECR`

The temporary branch decision matches the current web pipeline:

- the launch-ready app currently lives on `prod-ready-web-shell`
- after branch alignment, the production trigger should move back to `main`

### Environment needs

- ECR repository
- App Runner service per environment
- Secrets Manager or Parameter Store config
- CORS origin configured to the matching web domain

### Required GitHub configuration

- `AWS_REGION`
- `API_PROD_ECR_REPOSITORY`
- `API_PROD_APP_RUNNER_SERVICE_ARN`
- secret:
  - `AWS_DEPLOY_ROLE_ARN`

### Success criteria

- API boots cleanly
- `/health` responds
- content endpoints respond
- calendar endpoints respond

## Pipeline 3: Database

### Purpose

Keep schema changes controlled and production content bootstrapping explicit.

### What belongs here

- Flyway schema migrations
- controlled initial content bootstrap

### What does not belong here

- app-startup imports
- giant schema-plus-data Flyway seed files

### Strategy

#### Schema

- Flyway owns:
  - tables
  - indexes
  - constraints
  - future schema changes

#### Content

- initial content load is a separate bootstrap step
- load:
  - saints
  - prayers
  - novenas
  - related child rows
- verify row counts after load

Important rule:

- after bootstrap, PostgreSQL becomes the source of truth

### Success criteria

- new database can be created from scratch
- schema can be migrated through Flyway
- initial content can be loaded exactly once in a controlled way

## Production RDS Plan

### Initial requirements

- PostgreSQL version aligned with local development baseline
- automated backups enabled
- deletion protection enabled
- restricted network access
- credentials stored outside the repo

### Recommended launch stance

- single production RDS instance first
- multi-AZ if budget allows
- snapshots before major schema/content operations

### Database rollout order

1. create production RDS
2. configure API secrets
3. run Flyway
4. run initial content bootstrap
5. verify row counts and sample records
6. take a backup/snapshot

### First implementation document

The concrete first production database guide is:

- `docs/deployment/rds-production-bootstrap.md`

## Secrets And Config

Use environment-specific values for:

- datasource URL
- datasource username
- datasource password
- API base URLs
- frontend environment config
- allowed CORS origin
- future auth provider config

Recommended storage:

- `AWS Secrets Manager`
- `SSM Parameter Store`

Do not commit production secrets.

## Rollout Order

We should execute launch work slowly in this order:

1. finalize deployment architecture
2. implement Angular web pipeline
3. implement API pipeline
4. define Flyway + bootstrap database workflow
5. stand up `dev` infrastructure
6. deploy `dev`
7. verify `dev`
8. stand up `uat`
9. verify `uat`
10. stand up `prod`
11. run schema + content bootstrap
12. launch production

## Release Safety Rules

- production deploys require approval
- schema changes are versioned through Flyway
- content bootstrap is explicit and auditable
- backups exist before production data changes
- smoke tests run after every deploy

## Immediate Next Steps

We should do the following one step at a time:

1. create the Angular deployment pipeline plan in detail
2. implement the Angular production pipeline first
3. document the API container/deploy path
4. document the RDS + Flyway + bootstrap process
5. then move into real AWS environment creation
