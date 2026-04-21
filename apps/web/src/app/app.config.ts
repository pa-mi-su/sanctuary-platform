import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';
import { resolveSanctuaryApiBaseUrl, SANCTUARY_API_BASE_URL } from './core/api/sanctuary-api.config';
import { authTokenInterceptor } from './core/auth/auth-token.interceptor';
import { resolveSanctuaryAuthConfig, SANCTUARY_AUTH_CONFIG } from './core/auth/sanctuary-auth.config';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideHttpClient(withInterceptors([authTokenInterceptor])),
    provideRouter(routes),
    {
      provide: SANCTUARY_API_BASE_URL,
      useFactory: resolveSanctuaryApiBaseUrl,
    },
    {
      provide: SANCTUARY_AUTH_CONFIG,
      useFactory: resolveSanctuaryAuthConfig,
    },
  ],
};
