import { InjectionToken } from '@angular/core';

export const SANCTUARY_API_BASE_URL = new InjectionToken<string>('SANCTUARY_API_BASE_URL');
const PRODUCTION_API_BASE_URL = 'https://api.mydailysanctuary.com';
const DEV_API_BASE_URL = 'https://dev-api.mydailysanctuary.com';
const LOCAL_API_BASE_URL = 'http://localhost:8080';
const DEV_WEB_HOSTS = new Set(['dev.mydailysanctuary.com']);
const LOCAL_WEB_HOSTS = new Set(['localhost', '127.0.0.1']);

export function resolveSanctuaryApiBaseUrl(): string {
  if (typeof window !== 'undefined') {
    const apiTarget = new URLSearchParams(window.location.search).get('api');
    if (apiTarget === 'prod') {
      return PRODUCTION_API_BASE_URL;
    }
    if (apiTarget === 'dev') {
      return DEV_API_BASE_URL;
    }
    if (apiTarget === 'local') {
      return LOCAL_API_BASE_URL;
    }

    const hostname = window.location.hostname;
    if (DEV_WEB_HOSTS.has(hostname)) {
      return DEV_API_BASE_URL;
    }
    if (LOCAL_WEB_HOSTS.has(hostname)) {
      return LOCAL_API_BASE_URL;
    }
  }

  return PRODUCTION_API_BASE_URL;
}
