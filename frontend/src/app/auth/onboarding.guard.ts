import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

/**
 * Blocks PENDING users from accessing the main shell — redirects to /onboarding.
 */
export const onboardingCompletedGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const user = auth.currentUser();
  if (user && user.status === 'PENDING') {
    return router.createUrlTree(['/onboarding']);
  }
  return true;
};

/**
 * Only allows access to /onboarding for users that are not yet activated.
 * Already-active users are redirected to the dashboard.
 */
export const onboardingRequiredGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const user = auth.currentUser();
  if (!user) {
    return router.createUrlTree(['/login']);
  }
  if (user.status !== 'PENDING') {
    return router.createUrlTree(['/dashboard']);
  }
  return true;
};
