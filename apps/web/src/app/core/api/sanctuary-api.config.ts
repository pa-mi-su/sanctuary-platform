import { InjectionToken } from '@angular/core';

export const SANCTUARY_API_BASE_URL = new InjectionToken<string>('SANCTUARY_API_BASE_URL');
const PRODUCTION_API_BASE_URL = 'https://sa-d7fe5f77e3bd409caf712e69b701f1e8.ecs.us-east-1.on.aws';
const LOCAL_API_BASE_URL = 'http://localhost:8080';

export function resolveSanctuaryApiBaseUrl(): string {
  if (typeof window !== 'undefined') {
    const apiTarget = new URLSearchParams(window.location.search).get('api');
    if (apiTarget === 'prod') {
      return PRODUCTION_API_BASE_URL;
    }
    if (apiTarget === 'local') {
      return LOCAL_API_BASE_URL;
    }

    const hostname = window.location.hostname;
    if (hostname === 'localhost' || hostname === '127.0.0.1') {
      return LOCAL_API_BASE_URL;
    }
  }

  return PRODUCTION_API_BASE_URL;
}
