import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';

import { SANCTUARY_API_BASE_URL } from '../api/sanctuary-api.config';

export const authCookieInterceptor: HttpInterceptorFn = (request, next) => {
  const apiBaseUrl = inject(SANCTUARY_API_BASE_URL);

  if (!request.url.startsWith(apiBaseUrl)) {
    return next(request);
  }

  return next(request.clone({
    withCredentials: true,
  }));
};
