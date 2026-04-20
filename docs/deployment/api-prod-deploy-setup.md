# API Production Deploy Setup

This guide defines the first production deployment path for the Sanctuary Java API.

## Deployment Model

- source: `apps/api`
- build: Maven + Docker
- image registry: `ECR`
- runtime: `AWS App Runner`
- database: production `RDS PostgreSQL`

## Workflow

- `.github/workflows/api-prod-deploy.yml`

Current trigger:

- push to `prod-ready-web-shell`
- manual `workflow_dispatch`

This mirrors the temporary production workflow choice already used for the Angular app while the real product state still lives on `prod-ready-web-shell`.

## What The Workflow Does

1. check out the repo
2. set up Java 21
3. run `mvn -q test`
4. assume the AWS deploy role through GitHub OIDC
5. log in to ECR
6. build the API Docker image
7. push the image to ECR
8. trigger an App Runner deployment

## Required GitHub `prod` Environment Variables

- `AWS_REGION`
- `API_PROD_ECR_REPOSITORY`
- `API_PROD_APP_RUNNER_SERVICE_ARN`

## Required GitHub `prod` Environment Secret

- `AWS_DEPLOY_ROLE_ARN`

The same deploy role can be reused if it is granted:

- ECR push permissions
- App Runner deployment permissions

If you prefer tighter separation, create a second API-specific deploy role later.

## Required AWS Resources

### ECR

Create an ECR repository for the API image, for example:

- `sanctuary-api-prod`

### App Runner

Create an App Runner service that uses:

- the ECR image repository above
- container port `8080`

Recommended App Runner environment variables:

- `SPRING_PROFILES_ACTIVE=prod`
- `SANCTUARY_API_PORT=8080`
- `SANCTUARY_DB_URL`
- `SANCTUARY_DB_USERNAME`
- `SANCTUARY_DB_PASSWORD`

Use App Runner secrets integration or Secrets Manager/SSM wherever possible for database credentials.

## Important App Runner Behavior

The workflow triggers:

- `aws apprunner start-deployment`

That means the App Runner service should already exist and already be configured to pull from the correct ECR repository.

## Verification Checklist

After the first successful deploy:

1. hit `/health`
2. verify the service boots with the `prod` profile
3. verify database connectivity
4. verify Flyway runs successfully
5. verify a sample content endpoint returns data
