import { InjectionToken } from '@angular/core';

export const SANCTUARY_API_BASE_URL = new InjectionToken<string>('SANCTUARY_API_BASE_URL');

export function resolveSanctuaryApiBaseUrl(): string {
  const hostname = globalThis.location?.hostname ?? 'localhost';
  const protocol = globalThis.location?.protocol ?? 'https:';
  const host = globalThis.location?.host ?? 'localhost';

  if (hostname === 'localhost' || hostname === '127.0.0.1') {
    return 'http://localhost:8080';
  }

  if (hostname === 'mydailysanctuary.com' || hostname === 'www.mydailysanctuary.com') {
    return 'https://sa-d7fe5f77e3bd409caf712e69b701f1e8.ecs.us-east-1.on.aws';
  }

  return `${protocol}//${host}`;
}
