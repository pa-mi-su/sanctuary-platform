# DEV deployment follow-up

The initial post-merge DEV release failed before API deployment and before web file upload:

- `API Dev Pipeline` inherited `SANCTUARY_AUTH_ENABLED=true` in its deployment job. The native compatibility fixture then registered both a discovery-based and test JWT decoder; a disabled-auth security fixture also inherited enabled auth. Both test contexts now use private `MockEnvironment` instances, with explicit enabled/disabled settings. Production authentication behavior is unchanged. DEV PR tests now export enabled auth too so this difference cannot escape validation again.
- CloudFront rejected the custom response-header policy on DEV's existing Free pricing plan. DEV now uses AWS's fixed managed SecurityHeadersPolicy ID. No pricing plan is changed and no new cloud resource is needed. Production retains its custom-policy path.

The managed policy provides HSTS, nosniff, strict-origin-when-cross-origin and X-Frame-Options SAMEORIGIN. The previously built page CSP remains enforced through its HTML meta tag. Managed policy framing is SAMEORIGIN, rather than the planned custom policy's DENY/frame-ancestors none; it blocks cross-origin framing. The custom policy was never attached to DEV. This change does not claim that meta CSP enforces frame-ancestors.

Sources: [AWS plan feature table](https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/flat-rate-pricing-plan.html), [managed policy settings](https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/using-managed-response-headers-policies.html#managed-response-headers-policy-security).

Validation: full API suite under deployment-like auth environment variables: 95 tests, 94 passed, one existing calendar database audit skipped, no failures/errors. Six fake-AWS deployment-script tests verify DEV managed policy, unchanged production custom policy, environment/alias guards, config preservation, ETag handling and idempotence. These script tests cannot contact AWS.

Release sequence: start DEV through DEV Environment Control (branch dev, action start); merge the validated fix into dev; allow API and web DEV deployment workflows to complete; verify the API health, public news, anonymous admin denial, deployed headers and build commit. Owner then signs in with their Outlook administrator account and authenticator to test article editing/publishing. Use a draft first, and do not publish test content without an explicit owner action.

## Production launch alignment follow-up

The production web workflow now selects the same fixed AWS managed SecurityHeadersPolicy as DEV. The shared script accepts that managed policy for either supported environment and rejects a production target containing DEV aliases as well as the existing inverse check. Nine fake-AWS tests cover production managed-policy support, exact preservation of unrelated distribution settings, cross-environment/mixed-alias rejection, unknown environments, legacy custom-policy validation and idempotence. The production workflow runs these safeguards during validation and release. No production resources or pricing plans were changed while preparing this fix, and no MAIN pipeline was dispatched.
