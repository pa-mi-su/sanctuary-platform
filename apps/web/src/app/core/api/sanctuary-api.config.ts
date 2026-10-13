import { InjectionToken } from '@angular/core';

export const SANCTUARY_API_BASE_URL = new InjectionToken<string>('SANCTUARY_API_BASE_URL');
const PRODUCTION_API_BASE_URL = 'https://api.mydailysanctuary.com';
const DEV_API_BASE_URL = 'https://dev-api.mydailysanctuary.com';
const LOCAL_API_BASE_URL = 'http://localhost:8080';
const DEV_WEB_HOSTS = new Set(['dev.mydailysanctuary.com']);
const LOCAL_WEB_HOSTS = new Set(['localhost', '127.0.0.1']);

export function resolveSanctuaryApiBaseUrl(
  location: Pick<Location, 'hostname' | 'search'> | undefined = typeof window !== 'undefined'
    ? window.location
    : undefined,
): string {
  if (location) {
    // Keep browser/API hosts aligned so SameSite cookies work on local previews.
    const localBaseUrl = LOCAL_WEB_HOSTS.has(location.hostname)
      ? `http://${location.hostname}:8080`
      : LOCAL_API_BASE_URL;
    const apiTarget = new URLSearchParams(location.search).get('api');
    if (apiTarget === 'prod') {
      return PRODUCTION_API_BASE_URL;
    }
    if (apiTarget === 'dev') {
      return DEV_API_BASE_URL;
    }
    if (apiTarget === 'local') {
      return localBaseUrl;
    }

    const hostname = location.hostname;
    if (DEV_WEB_HOSTS.has(hostname)) {
      return DEV_API_BASE_URL;
    }
    if (LOCAL_WEB_HOSTS.has(hostname)) {
      return localBaseUrl;
    }
  }

  return PRODUCTION_API_BASE_URL;
}
