import { HttpClient, HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';

import { SANCTUARY_API_BASE_URL } from '../api/sanctuary-api.config';

export const authCookieInterceptor: HttpInterceptorFn = (request, next) => {
  const apiBaseUrl = inject(SANCTUARY_API_BASE_URL);
  const http = inject(HttpClient);

  if (!request.url.startsWith(apiBaseUrl)) {
    return next(request);
  }

  const credentialedRequest = request.clone({
    withCredentials: true,
  });

  if (isAuthEndpoint(request.url, apiBaseUrl)) {
    return next(credentialedRequest);
  }

  return next(credentialedRequest).pipe(
    catchError((error: unknown) => {
      if (!(error instanceof HttpErrorResponse) || error.status !== 401) {
        return throwError(() => error);
      }

      return http.post(`${apiBaseUrl}/auth/web/refresh`, null, { withCredentials: true }).pipe(
        switchMap(() => next(credentialedRequest)),
        catchError(() => throwError(() => error))
      );
    })
  );
};

function isAuthEndpoint(url: string, apiBaseUrl: string): boolean {
  const path = url.slice(apiBaseUrl.length);
  return path.startsWith('/auth/');
}
