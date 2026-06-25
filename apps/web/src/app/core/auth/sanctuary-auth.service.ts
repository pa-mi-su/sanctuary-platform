import { inject, Injectable, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import {
  AuthConfirmRegistrationRequest,
  AuthForgotPasswordRequest,
  AuthLoginRequest,
  AuthRegisterRequest,
  AuthResetPasswordRequest,
  AuthWebSessionResponse,
  SanctuaryApiService,
  UserProfile,
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
const REFRESH_TOKEN_KEY = 'sanctuary.auth.refreshToken';
const EXPIRES_AT_KEY = 'sanctuary.auth.expiresAt';

@Injectable({ providedIn: 'root' })
export class SanctuaryAuthService {
  private readonly api = inject(SanctuaryApiService);
  private readonly config = inject(SANCTUARY_AUTH_CONFIG);
  private refreshTimer: number | null = null;

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
    void this.restoreStoredSession();
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
      const session = await firstValueFrom(this.api.loginWeb(request));
      this.applyWebSession(session);
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

  async resetPassword(request: AuthResetPasswordRequest): Promise<void> {
    this.state.update((current) => ({ ...current, status: 'loading', message: null }));

    try {
      const session = await firstValueFrom(this.api.resetPasswordWeb(request));
      this.applyWebSession(session);
    } catch (error) {
      this.state.update((current) => ({ ...current, status: 'error' }));
      const message = this.extractMessage(error, 'We could not reset your password.');
      this.setError(message);
      throw error;
    }
  }

  logout(): void {
    this.api.logoutWebSession().subscribe({ error: () => undefined });
    this.clearStoredSession();
  }

  private async restoreStoredSession(): Promise<void> {
    this.clearLegacyTokenStorage();
    if (!this.config.enabled) {
      return;
    }

    this.state.update((current) => ({ ...current, status: 'loading', message: null }));

    try {
      const profile = await firstValueFrom(this.api.getMe());
      this.applyProfile(profile);
      return;
    } catch {
      // Try the refresh cookie once before treating the browser session as signed out.
    }

    try {
      const session = await firstValueFrom(this.api.refreshWebSession());
      this.applyWebSession(session);
    } catch {
      this.clearStoredSession();
    }
  }

  private applyWebSession(session: AuthWebSessionResponse): void {
    this.state.set({
      configured: this.config.enabled,
      status: 'authenticated',
      accessToken: null,
      idToken: null,
      email: session.email,
      displayName: session.displayName,
      message: null,
    });

    this.scheduleRefresh(session.expiresIn);
  }

  private applyProfile(profile: UserProfile): void {
    this.state.set({
      configured: this.config.enabled,
      status: 'authenticated',
      accessToken: null,
      idToken: null,
      email: profile.email,
      displayName:
        profile.displayName ??
        this.joinNames(profile.firstName ?? null, profile.lastName ?? null) ??
        profile.email,
      message: null,
    });
  }

  private clearStoredSession(): void {
    this.cancelScheduledRefresh();
    this.clearLegacyTokenStorage();
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

  private clearLegacyTokenStorage(): void {
    localStorage.removeItem(ACCESS_TOKEN_KEY);
    localStorage.removeItem(ID_TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
    localStorage.removeItem(EXPIRES_AT_KEY);
  }

  private scheduleRefresh(expiresIn: number): void {
    this.cancelScheduledRefresh();
    const delay = Math.max(expiresIn * 1000 - 60_000, 5_000);
    this.refreshTimer = window.setTimeout(() => {
      void this.refreshStoredSession();
    }, delay);
  }

  private cancelScheduledRefresh(): void {
    if (this.refreshTimer !== null) {
      window.clearTimeout(this.refreshTimer);
      this.refreshTimer = null;
    }
  }

  private async refreshStoredSession(): Promise<void> {
    try {
      const session = await firstValueFrom(this.api.refreshWebSession());
      this.applyWebSession(session);
    } catch {
      this.clearStoredSession();
    }
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
