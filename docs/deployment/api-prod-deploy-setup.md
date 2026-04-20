# API Production Deploy Setup

This guide defines the first production deployment path for the Sanctuary Java API.

## Deployment Model

- source: `apps/api`
- build: Maven + Docker
- image registry: `ECR`
- runtime: `Amazon ECS Express Mode`
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
8. deploy or update the API service through ECS Express Mode

## Required GitHub `prod` Environment Variables

- `AWS_REGION`
- `AWS_ACCOUNT_ID`
- `API_PROD_ECR_REPOSITORY`
- `API_PROD_ECS_SERVICE_NAME`
- `API_PROD_ECS_CLUSTER`

## Required GitHub `prod` Environment Secret

- `AWS_DEPLOY_ROLE_ARN`

The same deploy role can be reused if it is granted:

- ECR push permissions
- ECS deployment permissions

If you prefer tighter separation, create a second API-specific deploy role later.

## Required AWS Resources

### ECR

Create an ECR repository for the API image, for example:

- `sanctuary-api-prod`

### ECS Express Mode

Create an ECS Express Mode service that uses:

- the ECR image repository above
- container port `8080`

You will also need these IAM roles available in AWS:

- execution role:
  - typically `ecsTaskExecutionRole`
- infrastructure role:
  - typically `ecsInfrastructureRoleForExpressServices`
- task role:
  - for the first production deploy we intentionally reuse `ecsTaskExecutionRole` to keep the bootstrap simple
  - once the API needs AWS service access, split this into a dedicated app task role

Recommended ECS Express Mode environment variables:

- `SPRING_PROFILES_ACTIVE=prod`
- `SANCTUARY_API_PORT=8080`
- `SANCTUARY_DB_URL`
- `SANCTUARY_DB_USERNAME`
- `SANCTUARY_DB_PASSWORD`

Use ECS/Secrets Manager integration or SSM wherever possible for database credentials.

## Important ECS Express Mode Behavior

The workflow uses:

- `aws-actions/amazon-ecs-deploy-express-service@v1`

That action will deploy the image to the named ECS Express service and cluster.

## Current Bootstrap Assumption

The workflow now assumes the standard AWS-managed role names:

- `ecsTaskExecutionRole`
- `ecsInfrastructureRoleForExpressServices`

That keeps the GitHub configuration smaller and lets us bootstrap the first ECS deployment faster.

## Verification Checklist

After the first successful deploy:

1. hit `/health`
2. verify the service boots with the `prod` profile
3. verify database connectivity
4. verify Flyway runs successfully
5. verify a sample content endpoint returns data
