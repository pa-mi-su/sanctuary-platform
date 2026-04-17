import { provideHttpClient } from '@angular/common/http';
import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';
import { resolveSanctuaryApiBaseUrl, SANCTUARY_API_BASE_URL } from './core/api/sanctuary-api.config';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideHttpClient(),
    provideRouter(routes),
    {
      provide: SANCTUARY_API_BASE_URL,
      useFactory: resolveSanctuaryApiBaseUrl,
    },
  ],
};
