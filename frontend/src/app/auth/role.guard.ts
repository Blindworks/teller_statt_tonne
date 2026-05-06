import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';
import { RoleName } from '../users/user.model';

export function roleGuard(allowed: RoleName[]): CanActivateFn {
  return () => {
    const auth = inject(AuthService);
    const router = inject(Router);
    const roles = auth.currentUser()?.roles ?? [];
    if (roles.some((r) => allowed.includes(r))) {
      return true;
    }
    return router.createUrlTree(['/dashboard']);
  };
}
