import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { PermissionsService } from './permissions.service';

/**
 * Lässt den Zugriff zu, wenn das gegebene Feature für den aktuellen User freigegeben ist.
 * Falls die Permissions noch nicht geladen sind, wird zuerst geladen und dann entschieden.
 */
export function featureGuard(key: string): CanActivateFn {
  return async () => {
    const perms = inject(PermissionsService);
    const router = inject(Router);
    await perms.ensureLoaded();
    return perms.has(key) ? true : router.createUrlTree(['/dashboard']);
  };
}
