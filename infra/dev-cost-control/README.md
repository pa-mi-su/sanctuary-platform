# DEV cost-control infrastructure

This stack moves the automatic nightly DEV shutdown into AWS so it does not
depend on GitHub repository activity. It targets only:

- AWS account `160885294528`
- region `us-east-1`
- ECS cluster `sanctuary-dev`
- ECS service `sanctuary-api-dev`
- RDS instance `sanctuary-dev-db`

The EventBridge schedule runs at `03:23 America/New_York`, automatically
following daylight-saving-time changes. The Lambda scales the ECS service to
zero and stops the RDS instance. Repeated executions are safe when DEV is
already stopped.

## Safety and cost controls

- The CloudFormation template refuses any other AWS account or region.
- Lambda verifies the account, region, cluster ARN, service ARN, and database
  ARN before issuing mutations.
- IAM permissions name only the exact DEV resources.
- The schedule is `DISABLED` by default.
- Lambda concurrency is limited to one.
- Failed invocations go to an encrypted SQS dead-letter queue.
- Logs expire after 14 days.
- No continuously billed CloudWatch alarms are created.
- No customer-managed KMS key, NAT gateway, VPC attachment, or continuously
  running compute is created.

CloudFormation itself has no additional charge for standard AWS resources.
At one scheduled invocation per day, Scheduler and Lambda usage are far below
their published monthly free-tier request allowances. Logs are intentionally
small and retained for only 14 days.

## Deployment sequence

Do not remove the existing GitHub nightly schedule until this migration has
been fully verified.

The existing GitHub DEV application-deployment role is intentionally not given
permission to create IAM, Lambda, Scheduler, SQS, or CloudFormation resources.
GitHub validates the code and tests only. Run the guarded deployment script
using an authenticated AWS administrator session for this account.

1. Merge the infrastructure files through the normal branch process.
2. Validate the template:

   ```bash
   bash infra/dev-cost-control/deploy.sh validate
   ```

3. Deploy with the schedule disabled:

   ```bash
   bash infra/dev-cost-control/deploy.sh deploy-disabled
   ```

4. Confirm the CloudFormation stack is complete and its schedule is disabled.
5. Manually invoke `sanctuary-dev-nightly-stop` with:

   ```json
   {"action":"stop","source":"manual-verification"}
   ```

6. Confirm the exact DEV ECS service is at desired count zero, the exact DEV
   database is stopped or stopping, the Lambda log is successful, and the
   dead-letter queue is empty.
7. Enable the verified AWS schedule:

   ```bash
   bash infra/dev-cost-control/deploy.sh deploy-enabled
   ```

8. Confirm the first scheduled execution succeeds at 03:23 Eastern.
9. Only then remove the `schedule` trigger from
    `.github/workflows/dev-environment-control.yml`. Keep its manual `status`,
    `start`, and `stop` controls.

## Disable without deleting

Run `bash infra/dev-cost-control/deploy.sh deploy-disabled`. This changes only
the EventBridge schedule state. It does not start DEV, delete the database, or
remove logs.

## Rollback

Before cutover, the GitHub nightly stop remains the active fallback. If the AWS
schedule fails validation, leave it disabled and investigate the CloudFormation
events, Lambda logs, and dead-letter queue. Do not remove the GitHub
schedule until the AWS-native path has completed a real scheduled execution.
