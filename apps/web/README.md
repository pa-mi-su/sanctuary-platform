# Sanctuary Web

`apps/web` is Sanctuary's Angular 21 browser client. It is a responsive, signal-driven single-page shell over the shared Spring Boot API; `app.routes.ts` intentionally contains no Angular feature routes.

Current app version: `1.0.15`.

## Runtime behavior

`AppShellFacade` owns the browser experience and coordinates:

- tabs/views: home, novenas, intentions, patronage, liturgical calendar, saints, prayers, rosaries, About, account access, and Me
- day/week/month selection and date paging
- saint, novena, prayer, intention-term, and patronage-term search
- content detail modals and shared content URLs
- English, Spanish, and Polish language state
- Cognito-backed account flows through API endpoints
- protected actions that resume after login (favorite or start novena)
- profile/preferences, favorites, commitments, reminders, and account deletion
- anonymous novena progress stored locally, replaced by API-backed progress after authentication

The root template (`src/app/app.html`) composes page components based on facade state. This app does not use URL routing for its ordinary tab/page transitions. Share paths are generated as static preview documents at build time and are interpreted on load by the shell.

## Structure

```text
apps/web/
├── public/                       # Static assets, app-site association files
├── scripts/generate-share-previews.mjs
├── src/app/
│   ├── core/api/                 # Base-URL resolution and typed API service
│   ├── core/auth/                # Cookie session state and HTTP interceptor
│   ├── core/state/               # AppShellFacade and application state
│   └── pages/                    # Shell, feature, detail, auth, legal, footer UI
├── src/styles.scss
├── angular.json
├── package.json
└── README.md
```

## API selection

`src/app/core/api/sanctuary-api.config.ts` resolves the API in this order:

1. `?api=prod` -> `https://api.mydailysanctuary.com`
2. `?api=dev` -> `https://dev-api.mydailysanctuary.com`
3. `?api=local` -> `http://localhost:8080`
4. host `dev.mydailysanctuary.com` -> DEV API
5. host `localhost` or `127.0.0.1` -> local API
6. every other host -> production API

All calls are implemented by `SanctuaryApiService`; the browser never connects directly to PostgreSQL or AWS services.

## Web authentication

Web sessions use API-issued HttpOnly cookies:

- login: `POST /auth/web/login`
- refresh: `POST /auth/web/refresh`
- logout: `POST /auth/web/logout`
- password reset with immediate session: `POST /auth/web/reset-password`

`authCookieInterceptor` sets `withCredentials=true` for Sanctuary API requests. If a non-auth request returns `401`, it attempts one cookie refresh and retries the original request once.

The browser does **not** store access, ID, or refresh tokens. `SanctuaryAuthService` actively removes the four legacy token keys from `localStorage`; its in-memory token properties remain `null`. Cookie security, SameSite, domain, and refresh lifetime are controlled by the API profile.

## Sharing, privacy, and store links

`npm run build --workspace web` runs Angular compilation and then `scripts/generate-share-previews.mjs`. The generator:

- reads English saints, novenas, and prayers from `SANCTUARY_SHARE_PREVIEW_API_BASE_URL` (default `https://api.mydailysanctuary.com`)
- writes static preview documents for `/saints/{slug}`, `/novenas/{slug}`, and `/prayers/{slug}`
- writes a direct `/privacy` HTML document for store policy crawlers
- adds canonical, Open Graph, Twitter, and iOS smart-banner metadata
- defaults the site origin to `https://mydailysanctuary.com`

The footer links to:

- App Store app ID `6759986068`
- Google Play package `com.pamisu.sanctuary`
- Sanctuary's Facebook and Instagram profiles

Because preview generation reads live API content, the full production build requires network/API availability unless the generator environment variables point to another reachable API.

## Local development

From the repository root:

```bash
npm ci
npm start --workspace web
```

Open `http://localhost:4200`. For live data, start PostgreSQL and the API first:

```bash
docker compose up -d postgres
./apps/api/scripts/run-local.sh
```

To use a different API without rebuilding, append `?api=local`, `?api=dev`, or `?api=prod`.

## Test and build

```bash
# Non-watch unit test run
npm test --workspace web -- --watch=false

# Production Angular build plus static preview generation
npm run build --workspace web

# Development watch build
npm run watch --workspace web
```

Angular production budgets are:

- initial bundle: 500 kB warning, 1 MB error
- component style: 14 kB warning, 18 kB error

Output used by deployment is `apps/web/dist/web/browser`.

## CI/CD

- `.github/workflows/web-dev-deploy.yml`: PR validation for `dev`; push to `dev` builds and syncs the DEV S3 bucket, sets content types, and invalidates DEV CloudFront.
- `.github/workflows/web-prod-deploy.yml`: PR validation for `dev`, `uat`, `prod`, and `main`; push to `main` performs the production S3/CloudFront deployment.

Both workflows watch `apps/web/**`, root `package.json`, and root `package-lock.json`. Markdown-only changes under `apps/web` are excluded. A workflow YAML change can validate the workflow on a PR but is deliberately omitted from push deployment paths, so changing CI configuration alone does not redeploy the site.

Deployment credentials and S3/CloudFront targets are supplied by GitHub environments. Neither workflow deploys an API or native app.

## Source-of-truth files

- API selection: `src/app/core/api/sanctuary-api.config.ts`
- API contract: `src/app/core/api/sanctuary-api.service.ts`
- cookie session: `src/app/core/auth/sanctuary-auth.service.ts`
- retry/credentials behavior: `src/app/core/auth/auth-token.interceptor.ts`
- application orchestration: `src/app/core/state/app-shell.facade.ts`
- shell composition: `src/app/app.html`
- share/privacy generation: `scripts/generate-share-previews.mjs`
- deployment: `../../.github/workflows/web-dev-deploy.yml` and `../../.github/workflows/web-prod-deploy.yml`
