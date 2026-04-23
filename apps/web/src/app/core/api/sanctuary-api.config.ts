import { InjectionToken } from '@angular/core';

export const SANCTUARY_API_BASE_URL = new InjectionToken<string>('SANCTUARY_API_BASE_URL');
const PRODUCTION_API_BASE_URL = 'https://sa-d7fe5f77e3bd409caf712e69b701f1e8.ecs.us-east-1.on.aws';

export function resolveSanctuaryApiBaseUrl(): string {
  return PRODUCTION_API_BASE_URL;
}
