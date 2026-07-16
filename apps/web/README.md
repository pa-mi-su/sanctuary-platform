# Sanctuary Web

`apps/web` is the Angular web client for Sanctuary. It is the responsive browser experience and uses the same Spring Boot API as iOS and Android.

## Stack

- Angular 21
- TypeScript 5.9
- RxJS
- SCSS
- Vitest
- npm workspaces

## Product Areas

The web app includes:

- home
- liturgical calendar
- saints calendar, search, and detail modal
- novenas calendar, search, intention search, and detail modal
- prayers and rosary list/detail flows with category-aware API filtering
- account access screens
- Me/profile/about screens
- mobile navigation and responsive layouts
- build/version/environment display

## Structure

```text
apps/web/
├── public/                 # Static assets and runtime config files
├── src/app/
│   ├── core/api/           # API config and SanctuaryApiService
│   ├── core/auth/          # Auth service and token interceptor
│   ├── core/state/         # App shell state
│   └── pages/              # Feature page components
├── angular.json
├── package.json
└── README.md
```

## API And Auth

The API base URL is resolved in [`src/app/core/api/sanctuary-api.config.ts`](src/app/core/api/sanctuary-api.config.ts):

- localhost uses `http://localhost:8080`
- `dev.mydailysanctuary.com` uses `https://dev-api.mydailysanctuary.com`
- other hosts use `https://api.mydailysanctuary.com`
- `?api=local`, `?api=dev`, or `?api=prod` can override host selection for diagnostics

All API calls go through [`src/app/core/api/sanctuary-api.service.ts`](src/app/core/api/sanctuary-api.service.ts).

Auth state is managed by [`src/app/core/auth/sanctuary-auth.service.ts`](src/app/core/auth/sanctuary-auth.service.ts). Browser login, refresh, reset, and logout use the API's `/auth/web/**` endpoints and scoped `HttpOnly` cookies. [`src/app/core/auth/auth-token.interceptor.ts`](src/app/core/auth/auth-token.interceptor.ts) sends credentials and attempts one cookie refresh after a `401`; legacy tokens are removed from browser storage.

Authenticated favorites and novena commitments are stored through `/me/**`. The current novena-progress UI also keeps a local browser copy and reconciles it with the API after authentication.

`app.routes.ts` is intentionally empty. Page, modal, and share-path state is coordinated by `AppShellFacade` in the standalone application shell.

## Local Development

From the repo root:

```bash
npm start --workspace web
```

Open:

```text
http://localhost:4200
```

For a fully functional local web session, run the API first:

```bash
./apps/api/scripts/run-local.sh
```

## Build

From the repo root:

```bash
npm run build --workspace web
```

The build first writes Angular output to `apps/web/dist/web/browser`, then `scripts/generate-share-previews.mjs` fetches the English saint, novena, and prayer catalogs and writes extensionless share-preview route objects. The generator defaults to the production API and site, retries failed fetches, and makes the build network/API-dependent.

Override its inputs when needed:

```bash
SANCTUARY_SHARE_PREVIEW_API_BASE_URL=http://localhost:8080 \
SANCTUARY_SHARE_PREVIEW_SITE_ORIGIN=http://localhost:4200 \
npm run build --workspace web
```

`SANCTUARY_SHARE_PREVIEW_OUTPUT_DIR` can override the generated output directory.

## Tests

From the repo root:

```bash
npm test --workspace web -- --watch=false
```

The committed suite currently contains four Vitest assertions covering the application shell and footer.

## Deployment

The production workflow is [`../../.github/workflows/web-prod-deploy.yml`](../../.github/workflows/web-prod-deploy.yml).

Production flow:

1. build Angular
2. publish static assets to S3
3. invalidate CloudFront

Both deployment workflows also set JSON content types for Apple/Android association files and HTML content types for generated share routes. Dev deploys on `dev`; production deploys on `main`.

Related docs:

- [`../../docs/deployment/github-actions-web-prod-setup.md`](../../docs/deployment/github-actions-web-prod-setup.md)
- [`../../docs/architecture/deployment-and-pipelines.md`](../../docs/architecture/deployment-and-pipelines.md)
