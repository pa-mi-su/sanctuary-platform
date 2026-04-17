import { InjectionToken } from '@angular/core';

export const SANCTUARY_API_BASE_URL = new InjectionToken<string>('SANCTUARY_API_BASE_URL');

export function resolveSanctuaryApiBaseUrl(): string {
  const hostname = globalThis.location?.hostname ?? 'localhost';

  if (hostname === 'localhost' || hostname === '127.0.0.1') {
    return 'http://localhost:8080';
  }

  return `${globalThis.location.protocol}//${globalThis.location.host}`;
}
