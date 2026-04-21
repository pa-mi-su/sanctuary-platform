import { InjectionToken } from '@angular/core';

export interface SanctuaryAuthConfig {
  enabled: boolean;
  cognitoDomain: string;
  clientId: string;
  redirectUri: string;
  logoutUri: string;
  scopes: string[];
}

declare global {
  interface Window {
    SANCTUARY_AUTH_CONFIG?: Partial<SanctuaryAuthConfig>;
  }
}

export const SANCTUARY_AUTH_CONFIG = new InjectionToken<SanctuaryAuthConfig>('SANCTUARY_AUTH_CONFIG');

export function resolveSanctuaryAuthConfig(): SanctuaryAuthConfig {
  const origin = globalThis.location?.origin ?? 'http://localhost:4200';
  const runtimeConfig = globalThis.window?.SANCTUARY_AUTH_CONFIG ?? {};

  return {
    enabled: Boolean(runtimeConfig.enabled && runtimeConfig.cognitoDomain && runtimeConfig.clientId),
    cognitoDomain: String(runtimeConfig.cognitoDomain ?? '').replace(/\/$/, ''),
    clientId: String(runtimeConfig.clientId ?? ''),
    redirectUri: String(runtimeConfig.redirectUri ?? origin),
    logoutUri: String(runtimeConfig.logoutUri ?? origin),
    scopes: runtimeConfig.scopes ?? ['openid', 'email', 'profile'],
  };
}
