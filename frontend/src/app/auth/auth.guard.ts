import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

export const authGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (auth.isAuthenticated()) {
    return true;
  }
  const accessToken = auth.getAccessToken();
  if (accessToken && !auth.isAccessTokenExpired()) {
    return true;
  }
  if (accessToken && auth.isAccessTokenExpired() && !auth.getRefreshToken()) {
    auth.clearSession();
  }
  return router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
};
