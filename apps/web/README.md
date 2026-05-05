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
- non-localhost uses the production ECS API URL

All API calls go through [`src/app/core/api/sanctuary-api.service.ts`](src/app/core/api/sanctuary-api.service.ts).

Auth state is managed by [`src/app/core/auth/sanctuary-auth.service.ts`](src/app/core/auth/sanctuary-auth.service.ts). The web app stores session tokens locally and attaches bearer tokens through [`src/app/core/auth/auth-token.interceptor.ts`](src/app/core/auth/auth-token.interceptor.ts).

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

The production build outputs to Angular's configured `dist` folder.

## Tests

From the repo root:

```bash
npm test --workspace web
```

## Deployment

The production workflow is [`../../.github/workflows/web-prod-deploy.yml`](../../.github/workflows/web-prod-deploy.yml).

Production flow:

1. build Angular
2. publish static assets to S3
3. invalidate CloudFront

Related docs:

- [`../../docs/deployment/github-actions-web-prod-setup.md`](../../docs/deployment/github-actions-web-prod-setup.md)
- [`../../docs/architecture/deployment-and-pipelines.md`](../../docs/architecture/deployment-and-pipelines.md)
