import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';
import { RoleName, hasAnyRole } from '../users/user.model';

/**
 * Laesst den Zugriff zu, wenn der aktuelle User mindestens eine der angegebenen Rollen besitzt.
 * Falls der User nach einem Page-Reload noch nicht geladen ist, wird zuerst `/api/auth/me` abgewartet.
 */
export function roleGuard(...allowed: RoleName[]): CanActivateFn {
  return async () => {
    const auth = inject(AuthService);
    const router = inject(Router);
    const user = await auth.ensureUserLoaded();
    return hasAnyRole(user, ...allowed) ? true : router.createUrlTree(['/dashboard']);
  };
}
