# Sanctuary Auth, Cognito, Deploy, and Mobile UI Progress

Date: April 21, 2026

This document is a clean export of the work completed around authentication, Cognito, production deployment, and the latest mobile web UI fixes.

## 1. Architecture Direction

We moved away from a short-term auth approach and put a real long-term structure in place.

Current architecture:

- Amazon Cognito handles authentication
- Sanctuary backend/database handles user profile and app state
- Web uses Cognito Hosted UI for login/register
- Backend validates Cognito JWTs for authenticated user endpoints

This means:

- Cognito is the identity provider
- our database owns favorites, progress, preferences, and future user features

## 2. Backend User Foundation

In the backend, we added a proper internal user model instead of using raw Cognito identity as the only persistence key.

Implemented:

- real `users` table foundation
- `user_preferences`
- richer `/me` response model
- `PUT /me/preferences`
- account activity groundwork for future features
- favorites/progress tied to internal `users.id`

Long-term benefit:

- we can support profile fields, reminders, saved content, and future premium/user features without redesigning the data model later

## 3. Cognito Production Configuration

Confirmed production Cognito configuration:

- User pool ID: `us-east-1_syYJKg0NY`
- Web client ID: `7e3anthnuctm8p9nqck6kesjm9`
- Hosted UI domain:
  - `https://sanctuary-160885294528-prod.auth.us-east-1.amazoncognito.com`

Confirmed callback/logout URLs for the web client:

- `http://localhost:4200`
- `https://mydailysanctuary.com`

## 4. Backend Runtime Auth Wiring

Production ECS runtime was configured to run with Cognito-aware auth.

Important ECS environment variables:

- `SANCTUARY_AUTH_ENABLED=true`
- `SANCTUARY_COGNITO_ISSUER_URI=https://cognito-idp.us-east-1.amazonaws.com/us-east-1_syYJKg0NY`
- `SANCTUARY_COGNITO_CLIENT_ID=7e3anthnuctm8p9nqck6kesjm9`
- `SANCTUARY_DB_URL=jdbc:postgresql://sanctuary-prod-db.cwh6808ko1sb.us-east-1.rds.amazonaws.com:5432/sanctuary`
- `SANCTUARY_DB_USERNAME=sanctuary`
- `SANCTUARY_DB_PASSWORD` from AWS secret source
- `SPRING_PROFILES_ACTIVE=prod`

Effect:

- authenticated `/me`-style endpoints are enabled in production
- backend reads Cognito-issued access/id tokens
- database and auth are both wired at container startup

## 5. Production Deploy Pipeline Fixes

We repaired the production deployment flow so that validation and deployment are separated correctly.

Desired behavior:

- PRs into env branches validate code
- merge into `main` triggers real deployment

Final behavior:

- PRs into `dev`, `uat`, `prod`, and `main` run build/test checks
- merge/push to `main` runs the real deploy jobs

### Web deploy behavior on `main`

- build Angular app
- sync `apps/web/dist/web/browser` to S3
- invalidate CloudFront

### API deploy behavior on `main`

- run API tests
- build Docker image
- push image to ECR
- update ECS service
- start app with `prod` profile
- Flyway runs on startup automatically

## 6. ECS Secret Handling Fix

We had a deployment issue caused by misalignment between:

- what ECS was already using in AWS
- what GitHub Actions was trying to inject during deploy

The correct source of truth for the DB password in ECS is:

- `/sanctuary/prod/db/password`

Important fix:

- stopped relying on an extra GitHub indirection for the DB password secret path
- aligned the API deploy workflow directly to the same AWS secret path ECS already uses

Result:

- future API deploys are cleaner and more deterministic
- no duplicated DB secret strategy
- ECS and deploy workflow now agree on the same runtime secret source

## 7. Branch Promotion and Environment Alignment

We promoted the auth/deploy work through the env branches and then fixed branch alignment after a direct production workflow hotfix.

Relevant env branches:

- `dev`
- `uat`
- `prod`
- `main`

Normal promotion model:

- `feature -> dev -> uat -> prod -> main`

What happened here:

- product/auth code was promoted upward
- a deployment workflow issue surfaced on `main`
- we applied a direct production hotfix to `main`
- then back-ported the hotfix down:
  - `main -> prod`
  - `prod -> uat`
  - `uat -> dev`

Result:

- all env branches were brought back into alignment

## 8. Cognito Hosted UI Branding Work

You correctly pointed out that the login/register page did not match Sanctuary branding.

Important finding:

- this was not an Angular CSS problem
- this was the Cognito Hosted UI itself

Changing app CSS would not fix the hosted login page.

### What we did

We audited the real Sanctuary brand styling from the iOS app and applied a Cognito-compatible branded theme to the Hosted UI.

Used as the visual source of truth:

- iOS theme colors from the Sanctuary iOS app
- Sanctuary brand logo from the iOS assets

### Branding artifacts created

In the iOS repo (`/Users/pms/repos/Sanctuary`) we created:

- `docs/cognito/hosted-ui.css`
- `docs/cognito/brand-logo-cognito.png`
- `scripts/apply_cognito_branding.sh`

Purpose:

- keep Cognito Hosted UI branding under source control
- make future reapplication repeatable
- avoid one-off console-only styling changes

### Problems encountered during Cognito branding

#### Oversized uploaded logo

Original logo:

- `1024x1024`
- roughly `1.7 MB`

Cognito rejected it with HTTP `413`.

Fix:

- created a smaller Cognito-safe logo:
  - `brand-logo-cognito.png`

#### Cognito CSS selector restrictions

Cognito classic Hosted UI only allows a limited approved set of selectors.

It rejected selectors such as:

- `.banner-customizable img`
- `body`

So the final stylesheet had to use only Cognito-safe selectors such as:

- `.background-customizable`
- `.banner-customizable`
- `.logo-customizable`
- `.label-customizable`
- `.textDescription-customizable`
- `.inputField-customizable`
- `.submitButton-customizable`
- `.redirect-customizable`
- `.errorMessage-customizable`

### Current Cognito result

The Hosted UI now serves:

- custom Sanctuary-branded CSS
- custom Sanctuary logo asset

The login/register card area now reflects Sanctuary styling more closely:

- Sanctuary logo
- Sanctuary button styling
- Sanctuary field styling
- Sanctuary typography/color direction

### Known limitation

Cognito classic Hosted UI still does not allow full visual control.

That means:

- we can make it feel much closer to Sanctuary
- we cannot make it pixel-perfect like the app/web shell

Future option if exact visual parity is required:

- build custom login/register screens in the app/web
- keep Cognito only as the auth backend/provider

## 9. Current Production State

### In `sanctuary-platform`

Implemented and deployed:

- real auth/profile foundation
- user preferences foundation
- activity groundwork
- repaired deploy-on-main behavior
- repaired ECS/API secret handling

### In Cognito

Applied live:

- Sanctuary logo
- Sanctuary-themed Hosted UI CSS for login/register

## 10. Latest Web-Only Mobile UI Fixes

After the auth/Cognito work, we also fixed two mobile web issues in `sanctuary-platform`.

### Issue 1: Login/Register tab was cut off on mobile

Problem:

- top nav on iPhone Chrome was too tight
- `Login / Register` text overflowed and got cut off

Fix:

- on mobile, the unauthenticated tab now shows `Login`
- kept full `Login / Register` label on larger layouts
- tightened mobile header spacing and action-row sizing

Files changed:

- `apps/web/src/app/pages/app-header.component.ts`
- `apps/web/src/app/pages/app-header.component.scss`

### Issue 2: Liturgical calendar text overflowed tile boundaries on mobile

Problem:

- month/week tile copy was spilling out of the cards on narrow screens

Fix:

- added mobile-specific tile tightening
- clamped text inside tiles
- reduced tile padding/gaps/font sizes for small screens
- applied same tile-safety rules across:
  - liturgical
  - saints
  - novenas

Files changed:

- `apps/web/src/app/pages/liturgical-page.component.scss`
- `apps/web/src/app/pages/saints-page.component.scss`
- `apps/web/src/app/pages/novenas-page.component.scss`

### Verification

Verified locally:

- `npm run build --workspace web` passed

## 11. What Is Live vs What Is Branch-Only

### Live now

- production Cognito Hosted UI branding
- production backend/API deploy pipeline fixes
- production web deploy pipeline fixes
- production auth/runtime/ECS configuration

### Currently on branch only

Current branch:

- `feature/cognito-branding-fix`

Branch-only changes at time of this note:

- latest mobile web nav fix
- latest mobile calendar tile overflow fix

These should be tested and then committed/promoted through the normal branch flow.

## 12. Key Takeaways

1. The auth stack is now structured correctly for long-term growth.
2. Cognito Hosted UI can be branded well, but not perfectly.
3. Production deploys now work from `main` with Flyway on startup.
4. ECS secret handling is aligned to the real AWS runtime source of truth.
5. Mobile web still needed some responsive cleanup after the larger auth work, and those fixes are now in progress on the current branch.

## 13. Recommended Next Steps

1. Test the current mobile web fixes on iPhone Chrome
2. Commit the current `feature/cognito-branding-fix` branch in `sanctuary-platform`
3. Promote through:
   - `feature -> dev`
   - `dev -> uat`
   - `uat -> prod`
   - `prod -> main`
4. Decide whether Cognito Hosted UI is "good enough" visually
5. If not, the next real auth UX step is custom in-app/web login/register screens backed by Cognito APIs
