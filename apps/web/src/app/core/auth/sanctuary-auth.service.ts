import { inject, Injectable, signal } from '@angular/core';

import { SANCTUARY_AUTH_CONFIG, SanctuaryAuthConfig } from './sanctuary-auth.config';

type AuthStatus = 'signed-out' | 'loading' | 'authenticated' | 'error';

export interface SanctuaryAuthState {
  configured: boolean;
  status: AuthStatus;
  accessToken: string | null;
  idToken: string | null;
  email: string | null;
  displayName: string | null;
  message: string | null;
}

interface TokenResponse {
  access_token: string;
  id_token?: string;
  refresh_token?: string;
  expires_in: number;
  token_type: string;
}

const ACCESS_TOKEN_KEY = 'sanctuary.auth.accessToken';
const ID_TOKEN_KEY = 'sanctuary.auth.idToken';
const EXPIRES_AT_KEY = 'sanctuary.auth.expiresAt';
const PKCE_VERIFIER_KEY = 'sanctuary.auth.pkceVerifier';
const OAUTH_STATE_KEY = 'sanctuary.auth.oauthState';

@Injectable({ providedIn: 'root' })
export class SanctuaryAuthService {
  private readonly config = inject(SANCTUARY_AUTH_CONFIG);

  readonly state = signal<SanctuaryAuthState>({
    configured: this.config.enabled,
    status: 'signed-out',
    accessToken: null,
    idToken: null,
    email: null,
    displayName: null,
    message: this.config.enabled ? null : 'Cognito is not configured for this environment yet.',
  });

  constructor() {
    this.restoreStoredSession();
  }

  async completeRedirectIfPresent(): Promise<void> {
    const url = new URL(globalThis.location.href);
    const code = url.searchParams.get('code');
    const state = url.searchParams.get('state');
    const error = url.searchParams.get('error_description') ?? url.searchParams.get('error');

    if (error) {
      this.clearStoredSession();
      this.state.update((current) => ({ ...current, status: 'error', message: error }));
      this.removeOAuthQuery(url);
      return;
    }

    if (!code) {
      return;
    }

    if (!this.config.enabled) {
      this.state.update((current) => ({
        ...current,
        status: 'error',
        message: 'Cognito returned a login code, but auth is not configured in this environment.',
      }));
      this.removeOAuthQuery(url);
      return;
    }

    const expectedState = sessionStorage.getItem(OAUTH_STATE_KEY);
    const verifier = sessionStorage.getItem(PKCE_VERIFIER_KEY);
    sessionStorage.removeItem(OAUTH_STATE_KEY);
    sessionStorage.removeItem(PKCE_VERIFIER_KEY);

    if (!state || state !== expectedState || !verifier) {
      this.clearStoredSession();
      this.state.update((current) => ({
        ...current,
        status: 'error',
        message: 'Login state could not be verified. Please try again.',
      }));
      this.removeOAuthQuery(url);
      return;
    }

    this.state.update((current) => ({ ...current, status: 'loading', message: null }));

    try {
      const tokenResponse = await this.exchangeCodeForTokens(code, verifier);
      this.storeSession(tokenResponse);
      this.removeOAuthQuery(url);
    } catch {
      this.clearStoredSession();
      this.state.update((current) => ({
        ...current,
        status: 'error',
        message: 'Sanctuary could not complete sign in. Please try again.',
      }));
      this.removeOAuthQuery(url);
    }
  }

  async startLogin(): Promise<void> {
    await this.redirectToHostedUi(false);
  }

  async startRegister(): Promise<void> {
    await this.redirectToHostedUi(true);
  }

  logout(): void {
    this.clearStoredSession();

    if (!this.config.enabled) {
      return;
    }

    const logoutUrl = new URL(`${this.config.cognitoDomain}/logout`);
    logoutUrl.searchParams.set('client_id', this.config.clientId);
    logoutUrl.searchParams.set('logout_uri', this.config.logoutUri);
    globalThis.location.assign(logoutUrl.toString());
  }

  private async redirectToHostedUi(register: boolean): Promise<void> {
    if (!this.config.enabled) {
      this.state.update((current) => ({
        ...current,
        status: 'error',
        message: 'Cognito is not configured yet. Add the web user pool domain and app client before enabling login.',
      }));
      return;
    }

    const verifier = this.randomString(64);
    const state = this.randomString(32);
    const challenge = await this.sha256Base64Url(verifier);
    sessionStorage.setItem(PKCE_VERIFIER_KEY, verifier);
    sessionStorage.setItem(OAUTH_STATE_KEY, state);

    const loginUrl = new URL(`${this.config.cognitoDomain}/oauth2/authorize`);
    loginUrl.searchParams.set('client_id', this.config.clientId);
    loginUrl.searchParams.set('response_type', 'code');
    loginUrl.searchParams.set('scope', this.config.scopes.join(' '));
    loginUrl.searchParams.set('redirect_uri', this.config.redirectUri);
    loginUrl.searchParams.set('state', state);
    loginUrl.searchParams.set('code_challenge_method', 'S256');
    loginUrl.searchParams.set('code_challenge', challenge);
    if (register) {
      loginUrl.searchParams.set('screen_hint', 'signup');
    }

    globalThis.location.assign(loginUrl.toString());
  }

  private async exchangeCodeForTokens(code: string, verifier: string): Promise<TokenResponse> {
    const body = new URLSearchParams();
    body.set('grant_type', 'authorization_code');
    body.set('client_id', this.config.clientId);
    body.set('code', code);
    body.set('redirect_uri', this.config.redirectUri);
    body.set('code_verifier', verifier);

    const response = await fetch(`${this.config.cognitoDomain}/oauth2/token`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body,
    });

    if (!response.ok) {
      throw new Error('Token exchange failed.');
    }

    return response.json() as Promise<TokenResponse>;
  }

  private storeSession(tokenResponse: TokenResponse): void {
    const expiresAt = Date.now() + tokenResponse.expires_in * 1000;
    localStorage.setItem(ACCESS_TOKEN_KEY, tokenResponse.access_token);
    localStorage.setItem(ID_TOKEN_KEY, tokenResponse.id_token ?? '');
    localStorage.setItem(EXPIRES_AT_KEY, String(expiresAt));
    this.applyTokens(tokenResponse.access_token, tokenResponse.id_token ?? null, expiresAt);
  }

  private restoreStoredSession(): void {
    const accessToken = localStorage.getItem(ACCESS_TOKEN_KEY);
    const idToken = localStorage.getItem(ID_TOKEN_KEY);
    const expiresAt = Number(localStorage.getItem(EXPIRES_AT_KEY) ?? '0');

    if (!accessToken || !expiresAt || expiresAt <= Date.now()) {
      this.clearStoredSession();
      return;
    }

    this.applyTokens(accessToken, idToken, expiresAt);
  }

  private applyTokens(accessToken: string, idToken: string | null, expiresAt: number): void {
    const claims = this.decodeJwt(idToken || accessToken);
    this.state.set({
      configured: this.config.enabled,
      status: 'authenticated',
      accessToken,
      idToken,
      email: this.stringClaim(claims, 'email'),
      displayName:
        this.stringClaim(claims, 'name') ??
        this.stringClaim(claims, 'preferred_username') ??
        this.stringClaim(claims, 'cognito:username') ??
        this.stringClaim(claims, 'sub'),
      message: null,
    });

    window.setTimeout(() => this.clearStoredSession(), Math.max(expiresAt - Date.now(), 0));
  }

  private clearStoredSession(): void {
    localStorage.removeItem(ACCESS_TOKEN_KEY);
    localStorage.removeItem(ID_TOKEN_KEY);
    localStorage.removeItem(EXPIRES_AT_KEY);
    this.state.set({
      configured: this.config.enabled,
      status: 'signed-out',
      accessToken: null,
      idToken: null,
      email: null,
      displayName: null,
      message: this.config.enabled ? null : 'Cognito is not configured for this environment yet.',
    });
  }

  private removeOAuthQuery(url: URL): void {
    url.searchParams.delete('code');
    url.searchParams.delete('state');
    url.searchParams.delete('error');
    url.searchParams.delete('error_description');
    globalThis.history.replaceState({}, document.title, `${url.pathname}${url.search}${url.hash}`);
  }

  private decodeJwt(token: string | null): Record<string, unknown> {
    if (!token) {
      return {};
    }

    try {
      const payload = token.split('.')[1];
      if (!payload) {
        return {};
      }
      const normalized = payload.replace(/-/g, '+').replace(/_/g, '/');
      const json = decodeURIComponent(
        atob(normalized)
          .split('')
          .map((char) => `%${`00${char.charCodeAt(0).toString(16)}`.slice(-2)}`)
          .join('')
      );
      return JSON.parse(json) as Record<string, unknown>;
    } catch {
      return {};
    }
  }

  private stringClaim(claims: Record<string, unknown>, key: string): string | null {
    const value = claims[key];
    return typeof value === 'string' && value.length > 0 ? value : null;
  }

  private randomString(length: number): string {
    const bytes = new Uint8Array(length);
    crypto.getRandomValues(bytes);
    return Array.from(bytes, (byte) => `0${byte.toString(16)}`.slice(-2)).join('');
  }

  private async sha256Base64Url(value: string): Promise<string> {
    const digest = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(value));
    return btoa(String.fromCharCode(...new Uint8Array(digest)))
      .replace(/\+/g, '-')
      .replace(/\//g, '_')
      .replace(/=+$/, '');
  }
}
