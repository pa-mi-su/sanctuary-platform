import { inject, Injectable, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import {
  AuthConfirmRegistrationRequest,
  AuthForgotPasswordRequest,
  AuthLoginRequest,
  AuthRegisterRequest,
  AuthResetPasswordRequest,
  SanctuaryApiService,
} from '../api/sanctuary-api.service';
import { SANCTUARY_AUTH_CONFIG } from './sanctuary-auth.config';

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

const ACCESS_TOKEN_KEY = 'sanctuary.auth.accessToken';
const ID_TOKEN_KEY = 'sanctuary.auth.idToken';
const EXPIRES_AT_KEY = 'sanctuary.auth.expiresAt';

@Injectable({ providedIn: 'root' })
export class SanctuaryAuthService {
  private readonly api = inject(SanctuaryApiService);
  private readonly config = inject(SANCTUARY_AUTH_CONFIG);

  readonly state = signal<SanctuaryAuthState>({
    configured: this.config.enabled,
    status: 'signed-out',
    accessToken: null,
    idToken: null,
    email: null,
    displayName: null,
    message: this.config.enabled ? null : 'Authentication is not configured for this environment yet.',
  });

  constructor() {
    this.restoreStoredSession();
  }

  async completeRedirectIfPresent(): Promise<void> {
    return Promise.resolve();
  }

  async startLogin(): Promise<void> {
    return Promise.resolve();
  }

  async startRegister(): Promise<void> {
    return Promise.resolve();
  }

  async login(request: AuthLoginRequest): Promise<void> {
    if (!this.config.enabled) {
      this.setError('Authentication is not configured for this environment yet.');
      return;
    }

    this.state.update((current) => ({ ...current, status: 'loading', message: null }));

    try {
      const session = await firstValueFrom(this.api.login(request));
      this.storeSession(session.accessToken, session.idToken, session.expiresIn);
    } catch (error) {
      this.clearStoredSession();
      this.setError(this.extractMessage(error, 'Sanctuary could not sign you in.'));
      throw error;
    }
  }

  async register(request: AuthRegisterRequest): Promise<{ email: string; displayName: string }> {
    if (!this.config.enabled) {
      this.setError('Authentication is not configured for this environment yet.');
      throw new Error('Auth not configured');
    }

    this.state.update((current) => ({ ...current, status: 'loading', message: null }));

    try {
      const response = await firstValueFrom(this.api.register(request));
      this.state.update((current) => ({ ...current, status: 'signed-out', message: null }));
      return { email: response.email, displayName: response.displayName };
    } catch (error) {
      this.state.update((current) => ({ ...current, status: 'signed-out' }));
      this.setError(this.extractMessage(error, 'Sanctuary could not create your account.'));
      throw error;
    }
  }

  async confirmRegistration(request: AuthConfirmRegistrationRequest): Promise<string> {
    this.state.update((current) => ({ ...current, status: 'loading', message: null }));

    try {
      const response = await firstValueFrom(this.api.confirmRegistration(request));
      this.state.update((current) => ({ ...current, status: 'signed-out', message: response.message }));
      return response.message;
    } catch (error) {
      this.state.update((current) => ({ ...current, status: 'error' }));
      const message = this.extractMessage(error, 'We could not confirm your account.');
      this.setError(message);
      throw error;
    }
  }

  async resendConfirmation(email: string): Promise<string> {
    this.state.update((current) => ({ ...current, status: 'loading', message: null }));

    try {
      const response = await firstValueFrom(this.api.resendConfirmation({ email }));
      this.state.update((current) => ({ ...current, status: 'signed-out', message: response.message }));
      return response.message;
    } catch (error) {
      this.state.update((current) => ({ ...current, status: 'error' }));
      const message = this.extractMessage(error, 'We could not send another confirmation code.');
      this.setError(message);
      throw error;
    }
  }

  async forgotPassword(request: AuthForgotPasswordRequest): Promise<string> {
    this.state.update((current) => ({ ...current, status: 'loading', message: null }));

    try {
      const response = await firstValueFrom(this.api.forgotPassword(request));
      this.state.update((current) => ({ ...current, status: 'signed-out', message: response.message }));
      return response.message;
    } catch (error) {
      this.state.update((current) => ({ ...current, status: 'error' }));
      const message = this.extractMessage(error, 'We could not start password reset.');
      this.setError(message);
      throw error;
    }
  }

  async resetPassword(request: AuthResetPasswordRequest): Promise<string> {
    this.state.update((current) => ({ ...current, status: 'loading', message: null }));

    try {
      const response = await firstValueFrom(this.api.resetPassword(request));
      this.state.update((current) => ({ ...current, status: 'signed-out', message: response.message }));
      return response.message;
    } catch (error) {
      this.state.update((current) => ({ ...current, status: 'error' }));
      const message = this.extractMessage(error, 'We could not reset your password.');
      this.setError(message);
      throw error;
    }
  }

  logout(): void {
    this.clearStoredSession();
  }

  private storeSession(accessToken: string, idToken: string | null, expiresIn: number): void {
    const expiresAt = Date.now() + expiresIn * 1000;
    localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
    localStorage.setItem(ID_TOKEN_KEY, idToken ?? '');
    localStorage.setItem(EXPIRES_AT_KEY, String(expiresAt));
    this.applyTokens(accessToken, idToken, expiresAt);
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
        this.joinNames(this.stringClaim(claims, 'given_name'), this.stringClaim(claims, 'family_name')) ??
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
      message: this.config.enabled ? null : 'Authentication is not configured for this environment yet.',
    });
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

  private joinNames(firstName: string | null, lastName: string | null): string | null {
    if (firstName && lastName) {
      return `${firstName} ${lastName}`;
    }

    return firstName ?? lastName ?? null;
  }

  private setError(message: string): void {
    this.state.update((current) => ({ ...current, status: 'error', message }));
  }

  private extractMessage(error: unknown, fallback: string): string {
    if (typeof error === 'object' && error !== null) {
      const candidate = (error as { error?: { message?: string } }).error?.message;
      if (typeof candidate === 'string' && candidate.length > 0) {
        return candidate;
      }
    }

    return fallback;
  }
}
