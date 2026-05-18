# Auth Endpoint Hardening

Implemented after suspicious unauthenticated Cognito activity was traced to public auth requests passing through the production ECS API.

## Application protections

- Rate limits are enforced in the API for:
  - `POST /auth/register`: 5 requests per caller IP per hour
  - `POST /auth/login`: 20 requests per caller IP per 15 minutes
  - `POST /auth/forgot-password`: 5 requests per caller IP per hour
  - `POST /auth/resend-confirmation`: 5 requests per caller IP per hour
- Caller IP is resolved from `X-Forwarded-For`, then `X-Real-IP`, then `remoteAddr`.
- Auth requests are logged with endpoint, status, caller IP, forwarded IP header, user agent, email domain, and a non-reversible email hash.
- Logs do not include passwords, tokens, or full email addresses.
- Signup blocks obvious fake/test domains such as `example.com`, `example.org`, `example.net`, `test.com`, and `invalid.com`.

## AWS protections

- WAF Web ACL: `sanctuary-prod-auth-protection`
- WAF association: production Application Load Balancer `ecs-express-gateway-alb-7a79186d`
- WAF rules:
  - `RateLimitAuthRegister`: blocks over 100 `/auth/register` requests per IP per 5 minutes
  - `RateLimitAuthLogin`: blocks over 200 `/auth/login` requests per IP per 5 minutes
  - `RateLimitAuthForgotPassword`: blocks over 100 `/auth/forgot-password` requests per IP per 5 minutes
  - `RateLimitAuthResendConfirmation`: blocks over 100 `/auth/resend-confirmation` requests per IP per 5 minutes
- ALB access log bucket: `sanctuary-prod-alb-access-logs-160885294528-use1`
- ALB access log prefix: `prod-alb`
- ALB access logs expire after 30 days.
