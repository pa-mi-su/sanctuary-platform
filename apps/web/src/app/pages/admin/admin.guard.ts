import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';

import { SanctuaryApiService } from '../../core/api/sanctuary-api.service';

export const adminGuard: CanActivateFn = () => {
  const api = inject(SanctuaryApiService);
  const router = inject(Router);

  return api.listAdminUsers(1).pipe(
    map(() => true),
    catchError(() => of(router.createUrlTree(['/'])))
  );
};
