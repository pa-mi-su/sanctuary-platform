# Sanctuary Cognito Auth Setup

Sanctuary uses Cognito Hosted UI for web login and Cognito JWTs for API access.

## Backend Contract

Public endpoints remain public:

- `/health`
- `/actuator/**`
- `/calendar/**`
- `/content/**`

Authenticated user endpoints require a Cognito bearer token:

- `GET /me`
- `GET /me/favorites`
- `PUT /me/favorites/{itemType}/{itemId}`
- `DELETE /me/favorites/{itemType}/{itemId}`
- `GET /me/novena-commitments`
- `PUT /me/novena-commitments/{novenaId}`
- `DELETE /me/novena-commitments/{novenaId}`

## API Environment Variables

Set these on the ECS service when Cognito is ready:

```bash
SANCTUARY_AUTH_ENABLED=true
SANCTUARY_COGNITO_ISSUER_URI=https://cognito-idp.us-east-1.amazonaws.com/us-east-1_syYJKg0NY
SANCTUARY_COGNITO_CLIENT_ID=7e3anthnuctm8p9nqck6kesjm9
```

Local development defaults auth off so the API can still run without Cognito:

```bash
SANCTUARY_AUTH_ENABLED=false
```

## Web Runtime Config

The Angular app loads `/auth-config.js` before bootstrapping.

Default local file:

```js
window.SANCTUARY_AUTH_CONFIG = {
  enabled: false,
  cognitoDomain: "",
  clientId: "",
  redirectUri: window.location.origin,
  logoutUri: window.location.origin,
  scopes: ["openid", "email", "profile"]
};
```

Production should deploy an environment-specific `auth-config.js`:

```js
window.SANCTUARY_AUTH_CONFIG = {
  enabled: true,
  cognitoDomain: "https://sanctuary-160885294528-prod.auth.us-east-1.amazoncognito.com",
  clientId: "7e3anthnuctm8p9nqck6kesjm9",
  redirectUri: "https://mydailysanctuary.com",
  logoutUri: "https://mydailysanctuary.com",
  scopes: ["openid", "email", "profile"]
};
```

## Production Cognito Resources

- User pool name: `sanctuary-prod-users`
- User pool id: `us-east-1_syYJKg0NY`
- Issuer URI: `https://cognito-idp.us-east-1.amazonaws.com/us-east-1_syYJKg0NY`
- Web app client name: `sanctuary-web-prod`
- Web app client id: `7e3anthnuctm8p9nqck6kesjm9`
- Hosted UI domain: `https://sanctuary-160885294528-prod.auth.us-east-1.amazoncognito.com`

## Cognito App Client Settings

Use a public SPA app client:

- No client secret
- Authorization code grant
- PKCE enabled
- Callback URL: `https://mydailysanctuary.com`
- Sign-out URL: `https://mydailysanctuary.com`
- Local callback URL for development: `http://localhost:4200`
- Local sign-out URL for development: `http://localhost:4200`
- OAuth scopes: `openid`, `email`, `profile`

## Data Model

User identity is keyed by the Cognito JWT `sub` claim.

Existing tables:

- `user_favorites`
- `user_novena_commitments`

Flyway remains schema-only. User progress is created by authenticated API calls at runtime.
