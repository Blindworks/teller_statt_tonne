import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';
import { Role } from '../users/user.model';

export function roleGuard(allowed: Role[]): CanActivateFn {
  return () => {
    const auth = inject(AuthService);
    const router = inject(Router);
    const role = auth.currentUser()?.role ?? null;
    if (role && allowed.includes(role)) {
      return true;
    }
    return router.createUrlTree(['/dashboard']);
  };
}
