# GitHub Actions Setup For Web Production Deploy

This guide wires the Angular production deploy workflow to the existing Sanctuary AWS web hosting target.

Workflow:

- `.github/workflows/web-prod-deploy.yml`

Current production target:

- S3 bucket: `mydailysanctuary.com`
- CloudFront distribution: `E34NUCAHSMCJFM`
- AWS region: `us-east-1`

Repository:

- `pa-mi-su/sanctuary-platform`

Environment:

- `prod`

## What We Need

### In AWS

Create an IAM role for GitHub Actions that:

- trusts GitHub OIDC
- allows deploy access only to:
  - bucket `mydailysanctuary.com`
  - CloudFront distribution `E34NUCAHSMCJFM`

### In GitHub

Add these values for the `prod` environment:

- variable: `AWS_REGION=us-east-1`
- variable: `WEB_PROD_S3_BUCKET=mydailysanctuary.com`
- variable: `WEB_PROD_CLOUDFRONT_DISTRIBUTION_ID=E34NUCAHSMCJFM`
- secret: `AWS_DEPLOY_ROLE_ARN=<the IAM role ARN>`

## Step 1: Create The GitHub OIDC IAM Identity Provider

Only do this once per AWS account.

In AWS IAM:

1. Open `IAM`
2. Go to `Identity providers`
3. Click `Add provider`
4. Provider type: `OpenID Connect`
5. Provider URL:
   - `https://token.actions.githubusercontent.com`
6. Audience:
   - `sts.amazonaws.com`

## Step 2: Create The IAM Role

In AWS IAM:

1. Create a new role
2. Trusted entity type:
   - `Web identity`
3. Identity provider:
   - `token.actions.githubusercontent.com`
4. Audience:
   - `sts.amazonaws.com`
5. After role creation, replace the trust policy with:
   - `docs/deployment/aws-github-oidc-trust-policy.json`

Important:

- this trust policy is locked to:
  - repo `pa-mi-su/sanctuary-platform`
  - GitHub Actions environment `prod`

That means the workflow can only assume the role when the job runs with:

- `environment: prod`

## Step 3: Attach The Deploy Permissions Policy

Attach the permissions policy from:

- `docs/deployment/aws-web-prod-deploy-permissions-policy.json`

This policy allows:

- list/sync/delete objects in `mydailysanctuary.com`
- create CloudFront invalidations on distribution `E34NUCAHSMCJFM`

## Step 4: Copy The Role ARN

After the role exists, copy its ARN.

It will look like:

```text
arn:aws:iam::<account-id>:role/github-actions-sanctuary-web-prod-deploy
```

## Step 5: Configure GitHub Environment Values

In GitHub:

1. Open repository:
   - `pa-mi-su/sanctuary-platform`
2. Go to:
   - `Settings`
   - `Environments`
   - `prod`
3. Create the environment if it does not exist

Add environment variables:

```text
AWS_REGION=us-east-1
WEB_PROD_S3_BUCKET=mydailysanctuary.com
WEB_PROD_CLOUDFRONT_DISTRIBUTION_ID=E34NUCAHSMCJFM
```

Add environment secret:

```text
AWS_DEPLOY_ROLE_ARN=<paste role arn here>
```

Recommended:

- add required reviewers for the `prod` environment before automatic deploys are allowed

## Step 6: Trigger The Workflow

Options:

- merge/push to `main`
- or run manually from GitHub Actions:
  - `Web Prod Deploy`

## What The Workflow Does

The workflow will:

1. check out the repo
2. install dependencies with `npm ci`
3. build Angular with:
   - `npm run build --workspace web`
4. sync `apps/web/dist/web/browser` to:
   - `s3://mydailysanctuary.com`
5. invalidate:
   - `E34NUCAHSMCJFM`

## Important Warning

This workflow intentionally replaces the current live static site content in:

- `mydailysanctuary.com`

That is expected and matches the current rollout decision.

## Recommended Verification After First Deploy

After the first successful run:

1. open `mydailysanctuary.com`
2. verify the Angular app loads
3. refresh a deep-linked page to confirm SPA fallback behavior
4. verify `About`, `Support`, and `Privacy` still work
5. verify the app loads static assets correctly
