# Sanctuary Session Storage Audit and HttpOnly Cookie Migration Plan

Date: 2026-05-27  
Branch: `codex/session-storage-audit`  
Scope: Audit plus implementation plan. This branch now includes the first implementation pass for Angular web HttpOnly cookie auth.

## Executive Summary

The audit found that Android is not storing session tokens in browser-style local storage. Android currently stores the Cognito session in `EncryptedSharedPreferences`, backed by AndroidX Security and a `MasterKey`. iOS stores session data in the Apple Keychain.

The real security concern is the Angular web app. The web client stores Cognito JWT session data in browser `localStorage`:

- `sanctuary.auth.accessToken`
- `sanctuary.auth.idToken`
- `sanctuary.auth.refreshToken`
- `sanctuary.auth.expiresAt`

This is a common single-page application pattern, but it is weaker than an HttpOnly cookie design because JavaScript can read `localStorage`. If the web app ever had an XSS vulnerability, attacker-controlled JavaScript could potentially read and exfiltrate the tokens.

The recommended fix is to move web session ownership from Angular to the Spring Boot backend using `HttpOnly`, `Secure`, `SameSite` cookies, while leaving mobile token storage unchanged.

## Implementation Status On This Branch

Implemented in this branch:

- Added web-specific backend auth endpoints:
  - `POST /auth/web/login`
  - `POST /auth/web/refresh`
  - `POST /auth/web/logout`
- Preserved existing mobile token endpoints:
  - `POST /auth/login`
  - `POST /auth/refresh`
- Added backend HttpOnly cookie setting/clearing for the web session.
- Added a Spring Security bearer token resolver that keeps supporting `Authorization: Bearer <jwt>` and also accepts the web JWT from the `sanctuary_id` cookie.
- Enabled credentialed CORS for the existing explicit Sanctuary origins.
- Updated Angular web auth so it no longer writes Cognito JWTs to `localStorage`.
- Updated Angular web requests to use `withCredentials`.
- Updated Angular web session bootstrap to use `/me` and cookie refresh instead of reading tokens from browser storage.
- Bumped Angular web version from `1.0.9` to `1.0.10`.

Not changed:

- Android secure storage remains `EncryptedSharedPreferences`.
- iOS secure storage remains Keychain.
- Mobile clients can continue using Bearer tokens.
- API Maven artifact version remains `0.0.1-SNAPSHOT`; backend deploys are versioned by container image/task definition/commit in the current deployment model.

Verification run:

- `npm run build --workspace web` passed.
- `npm test --workspace web -- --watch=false` passed.
- `mvn -q -Denforcer.skip=true test` passed.
- `git diff --check` passed.
- `mvn -q -DskipTests compile` was blocked locally because this machine does not have Java 21 installed. The project enforcer requires Java 21. Compile and tests passed on the installed Java runtime when the local enforcer was skipped.

## Current Architecture

### Backend Authentication Flow

The backend delegates authentication to Amazon Cognito.

Relevant files:

- `apps/api/src/main/java/app/sanctuary/api/auth/web/AuthController.java`
- `apps/api/src/main/java/app/sanctuary/api/auth/service/CognitoAuthService.java`
- `apps/api/src/main/java/app/sanctuary/api/config/SecurityConfig.java`

Current login flow:

1. Client calls `POST /auth/login`.
2. Spring Boot calls Cognito using `USER_PASSWORD_AUTH`.
3. Cognito returns:
   - `accessToken`
   - `idToken`
   - `refreshToken`
   - `tokenType`
   - `expiresIn`
4. Backend returns those tokens to the client as JSON.
5. Client stores and reuses the tokens.

Current protected request flow:

1. Client calls a protected route under `/me/**`.
2. Client sends a JWT in the `Authorization` header:

   ```http
   Authorization: Bearer <jwt>
   ```

3. Spring Security OAuth2 Resource Server validates the JWT.
4. The controller receives an authenticated `Authentication`.
5. `CurrentUser.from(authentication)` extracts Cognito claims such as `sub`, `email`, `given_name`, and `family_name`.
6. The backend uses that user identity to scope database operations to the authenticated account.

### Backend JWT Validation

Sanctuary uses Spring Security as an OAuth2 Resource Server:

```java
.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
```

The backend validates:

- JWT signature using Cognito public keys
- issuer
- expiration
- audience or Cognito app client id

Route authorization:

- `/health`, `/actuator/**`, `/auth/**`, `/calendar/**`, `/content/**` are public.
- `/me/**` requires authentication.
- everything else is denied.

## Platform Storage Findings

### Android

Finding: Android does not use browser local storage for tokens.

Relevant file:

`apps/android/app/src/main/java/app/sanctuary/android/data/SessionRepository.kt`

Current storage:

```kotlin
EncryptedSharedPreferences.create(
    context,
    "sanctuary_session",
    MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build(),
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
)
```

Stored session contents:

```kotlin
data class StoredSession(
    val accessToken: String,
    val idToken: String,
    val refreshToken: String?,
    val tokenType: String,
    val expiresAtMillis: Long,
    val email: String,
    val displayName: String
)
```

Current Android protected request behavior:

- Android reads the encrypted stored session.
- It uses `idToken` first, falling back to `accessToken`.
- It sends:

  ```http
  Authorization: Bearer <token>
  ```

Assessment:

Android is already using native encrypted storage. This is acceptable for a mobile app and should not be replaced with cookies.

### iOS

Finding: iOS stores tokens in Keychain-backed storage.

Relevant files:

- `apps/ios/Sanctuary/Core/Application/AccountSessionStore.swift`
- `apps/ios/Sanctuary/Core/Application/KeychainStore.swift`
- `apps/ios/Sanctuary/Core/Data/API/SanctuaryAPIClient.swift`

Current storage:

```swift
KeychainStore(service: "com.pamisu.Sanctuary.session")
```

Keychain accessibility:

```swift
kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
```

Current iOS protected request behavior:

```swift
request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
```

Assessment:

iOS is already using platform secure storage. This is appropriate for a native mobile app and should not be replaced with cookies.

### Angular Web

Finding: Angular stores Cognito session tokens in browser `localStorage`.

Relevant files:

- `apps/web/src/app/core/auth/sanctuary-auth.service.ts`
- `apps/web/src/app/core/auth/auth-token.interceptor.ts`

Current web storage:

```ts
localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
localStorage.setItem(ID_TOKEN_KEY, idToken ?? '');
localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
localStorage.setItem(EXPIRES_AT_KEY, String(expiresAt));
```

Current web restore:

```ts
const accessToken = localStorage.getItem(ACCESS_TOKEN_KEY);
const idToken = localStorage.getItem(ID_TOKEN_KEY);
const refreshToken = localStorage.getItem(REFRESH_TOKEN_KEY);
const expiresAt = Number(localStorage.getItem(EXPIRES_AT_KEY) ?? '0');
```

Current web protected request behavior:

```ts
const token = auth.state().idToken ?? auth.state().accessToken;

return next(request.clone({
  setHeaders: {
    Authorization: `Bearer ${token}`,
  },
}));
```

Assessment:

This is the highest-risk storage pattern in the current system. It works, but browser JavaScript can read these tokens. The refresh token is especially sensitive because it can be used to mint new access/id tokens.

## Risk Explanation

The main issue is not that Cognito or JWTs are wrong. The issue is where the browser client stores them.

With `localStorage`:

- tokens persist across reloads
- Angular can easily read them
- malicious JavaScript from an XSS vulnerability could also read them

With `HttpOnly` cookies:

- browser stores the session token
- JavaScript cannot read it
- browser sends it automatically with eligible requests
- token theft from XSS becomes much harder

HttpOnly cookies do not eliminate every risk. They trade direct token theft risk for needing stronger CSRF and CORS controls.

## Recommended Target Design

Use a hybrid auth model:

- Mobile keeps Bearer tokens in native secure storage.
- Web uses `HttpOnly`, `Secure`, `SameSite` cookies.
- Backend supports both:
  - `Authorization: Bearer <jwt>` for iOS and Android
  - cookie-carried JWT for Angular web

### Target Web Login Flow

Current:

```text
Angular -> POST /auth/login
Backend -> Cognito
Cognito -> tokens
Backend -> returns tokens as JSON
Angular -> stores tokens in localStorage
```

Target:

```text
Angular -> POST /auth/login
Backend -> Cognito
Cognito -> tokens
Backend -> sets HttpOnly Secure cookies
Angular -> receives no raw tokens
Angular -> calls /me to load user profile
```

### Target Cookie Set

Proposed cookies:

- `sanctuary_access`
- `sanctuary_id`
- `sanctuary_refresh`

Recommended attributes:

```text
HttpOnly
Secure
SameSite
Path
Max-Age
```

The refresh cookie should be narrower if possible:

```text
Path=/auth/refresh
```

The access/id cookie can be scoped to authenticated API paths:

```text
Path=/me
```

Exact `SameSite` choice depends on final API hosting:

- If web and API are same-site, use `SameSite=Lax` where possible.
- If web and API remain cross-site, cookies may require `SameSite=None; Secure`.

Current production web origin:

```text
https://mydailysanctuary.com
```

Current API appears to be on an ECS domain:

```text
https://sa-...ecs.us-east-1.on.aws
```

Because those are cross-site, a production cookie migration should also consider using a first-party API hostname such as:

```text
https://api.mydailysanctuary.com
```

That would make cookie policy and browser behavior cleaner.

## Backend Changes Needed

### 1. Add Web Cookie Login Response

Update `/auth/login` so the backend can set cookies for browser clients.

Possible approach:

- Keep existing JSON token response for mobile.
- Add a web-specific mode based on request header or endpoint.

Options:

```text
POST /auth/login
POST /auth/web/login
```

or:

```http
X-Sanctuary-Client: web
```

Safer and clearer option:

```text
POST /auth/web/login
POST /auth/web/refresh
POST /auth/web/logout
```

This avoids breaking mobile clients.

### 2. Set Cookies Server-Side

After Cognito login succeeds, the backend sets cookies instead of returning raw tokens to Angular.

Backend response should include:

```http
Set-Cookie: sanctuary_id=<jwt>; HttpOnly; Secure; SameSite=...; Path=/me; Max-Age=...
Set-Cookie: sanctuary_refresh=<refresh>; HttpOnly; Secure; SameSite=...; Path=/auth/web/refresh; Max-Age=...
```

### 3. Add Cookie-to-JWT Authentication Filter

Spring Security currently validates Bearer tokens from the `Authorization` header.

For web cookies, add a filter or bearer token resolver that:

1. Checks for an `Authorization` header first.
2. If absent, checks the auth cookie.
3. Extracts the JWT from the cookie.
4. Lets Spring Security validate it using the existing JWT decoder.

This preserves the same validation path:

- Cognito signature validation
- issuer validation
- expiration validation
- audience/client id validation

### 4. Add Web Refresh Endpoint

Current refresh flow expects JSON:

```json
{
  "refreshToken": "..."
}
```

Target web flow:

```text
Browser sends sanctuary_refresh cookie
Backend calls Cognito REFRESH_TOKEN_AUTH
Backend sets new HttpOnly cookies
Angular receives no raw tokens
```

### 5. Add Web Logout Endpoint

Add or update logout behavior to clear cookies:

```http
Set-Cookie: sanctuary_id=; Max-Age=0
Set-Cookie: sanctuary_refresh=; Max-Age=0
```

Optional:

- call Cognito global sign-out if an access token is available
- clear app-side session state

### 6. Update CORS for Credentials

Current backend CORS allows origins and headers, but does not explicitly allow credentials.

Current file:

`apps/api/src/main/java/app/sanctuary/api/config/WebConfig.java`

Current behavior:

```java
.allowedOrigins(
    "http://localhost:4200",
    "http://127.0.0.1:4200",
    "https://mydailysanctuary.com",
    "https://www.mydailysanctuary.com"
)
.allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
.allowedHeaders("*");
```

Cookie auth from Angular will require:

```java
.allowCredentials(true)
```

Angular requests will require:

```ts
withCredentials: true
```

### 7. Add CSRF Strategy

Because cookies are sent automatically by the browser, state-changing requests need CSRF consideration.

Recommended approach:

- keep strict allowed origins
- use `SameSite=Lax` if same-site hosting is possible
- if cross-site cookies are required, add CSRF tokens for unsafe methods:
  - POST
  - PUT
  - PATCH
  - DELETE

Possible implementation:

- backend issues a readable CSRF cookie such as `XSRF-TOKEN`
- Angular sends `X-XSRF-TOKEN` header
- backend validates header matches cookie

This CSRF token is not a session token and can be readable by JavaScript.

## Angular Changes Needed

### 1. Remove Token Persistence

Remove auth token storage from `localStorage`:

- `ACCESS_TOKEN_KEY`
- `ID_TOKEN_KEY`
- `REFRESH_TOKEN_KEY`
- `EXPIRES_AT_KEY`

Angular should not persist JWTs.

### 2. Remove Bearer Token Interceptor for Web

Current:

```ts
Authorization: `Bearer ${token}`
```

Target:

```text
No Authorization header for browser session.
Browser sends HttpOnly cookies automatically.
```

### 3. Use Credentials on API Calls

Angular HTTP calls to the API should include credentials:

```ts
withCredentials: true
```

This can be added globally via an interceptor or on the API service calls.

### 4. Restore Session by Calling `/me`

Current web bootstrapping restores tokens from `localStorage`.

Target bootstrapping:

```text
App starts
Angular calls GET /me with credentials
If 200, user is authenticated
If 401, user is signed out
```

Angular state should keep only display/session state:

- authenticated/signed-out/loading
- email
- displayName
- user profile

It should not hold long-lived JWTs.

### 5. Refresh Without Reading Tokens

Current:

```text
Angular reads refreshToken from localStorage
Angular sends refreshToken in JSON
```

Target:

```text
Angular calls POST /auth/web/refresh with credentials
Backend reads refresh cookie
Backend sets new cookies
Angular updates UI state from response or /me
```

## Mobile Changes Needed

No mobile storage migration is recommended.

Android and iOS should continue using:

- Android: `EncryptedSharedPreferences`
- iOS: Keychain
- Bearer token headers

If the backend adds cookie support, it must preserve Bearer token support so mobile apps do not break.

## Testing Plan

### Backend Tests

Add tests for:

- web login sets cookies
- web login does not expose raw tokens in response body
- web refresh reads refresh cookie and sets new cookies
- web logout clears cookies
- `/me/**` accepts cookie JWT
- `/me/**` still accepts Bearer JWT for mobile
- invalid cookie JWT is rejected
- missing auth is rejected for `/me/**`
- public routes remain public

### Angular Tests

Add/update tests for:

- no JWT keys are written to `localStorage`
- auth bootstrap calls `/me`
- login uses cookie-based endpoint
- logout calls backend and clears client auth state
- `/me/**` requests use credentials
- Bearer token interceptor is removed or disabled for web

### Manual Verification

In browser DevTools:

- Application tab should show no JWTs in `localStorage`.
- Cookies should show session cookies marked `HttpOnly`.
- JavaScript should not be able to read the session cookies.
- Network requests to `/me/**` should include cookies.
- Reloading the app should keep the user signed in.
- Logout should remove cookies and return the app to signed-out state.

### Security Verification

Verify:

- `HttpOnly` is present
- `Secure` is present
- `SameSite` is correct for deployed domain strategy
- CORS only allows approved Sanctuary origins
- credentialed CORS is not wildcarded
- CSRF handling is present for unsafe methods if cross-site cookies are used

## Interview Explanation

We audited how Sanctuary stored authenticated sessions across each client. The initial concern was that session tokens might be stored insecurely everywhere. The audit showed a more precise picture: iOS was already using Keychain, Android was already using AndroidX `EncryptedSharedPreferences`, and the real weakness was the Angular web app storing Cognito access, ID, and refresh tokens in browser `localStorage`.

The risk was that `localStorage` is readable by JavaScript. If the web app ever had an XSS issue, an attacker could potentially extract the JWTs, especially the refresh token. The fix we planned was to move browser session handling to backend-managed `HttpOnly`, `Secure` cookies. Angular would stop reading or storing JWTs and would instead call authenticated endpoints with credentials. The backend would read the JWT from the cookie, validate it using the same Spring Security OAuth2 Resource Server configuration, and still support Bearer tokens for iOS and Android.

The important design decision was not to break mobile. Mobile secure storage is appropriate for native apps, so the migration should be web-only. The backend should support both authorization styles: Bearer tokens for mobile and HttpOnly cookie JWTs for the browser.

## Proposed Implementation Phases

### Phase 1: Backend Cookie Support

- Add web-specific login, refresh, and logout endpoints.
- Set `HttpOnly`, `Secure`, `SameSite` cookies.
- Add cookie-based JWT extraction while preserving Bearer auth.
- Configure credentialed CORS safely.
- Add backend tests.

### Phase 2: Angular Migration

- Remove token storage from `localStorage`.
- Remove web Bearer token interceptor behavior.
- Add `withCredentials` for API calls.
- Bootstrap session through `/me`.
- Update login, refresh, and logout flows.
- Add Angular tests.

### Phase 3: Security Hardening

- Decide final same-site API domain strategy.
- Add CSRF protection if needed.
- Verify cookies in browser DevTools.
- Verify no JWTs remain in `localStorage`.
- Run end-to-end login, refresh, reload, and logout tests.

### Phase 4: Rollout

- Deploy backend support first while existing Angular still works.
- Deploy Angular cookie migration after backend is ready.
- Confirm mobile clients still authenticate with Bearer tokens.
- Monitor auth errors, CORS failures, and `/me/**` request failures.
