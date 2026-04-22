import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';

import { SanctuaryAuthService } from './sanctuary-auth.service';

export const authTokenInterceptor: HttpInterceptorFn = (request, next) => {
  const auth = inject(SanctuaryAuthService);
  const token = auth.state().idToken ?? auth.state().accessToken;

  if (!token || !request.url.includes('/me')) {
    return next(request);
  }

  return next(request.clone({
    setHeaders: {
      Authorization: `Bearer ${token}`,
    },
  }));
};
