import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map, switchMap, of } from 'rxjs';
import { AuthService } from './auth.service';

export const authGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (auth.isAuthenticated()) {
    return true;
  }

  const toLogin = () =>
    router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });

  const accessToken = auth.getAccessToken();
  if (accessToken && !auth.isAccessTokenExpired()) {
    return auth.reloadCurrentUser().pipe(map((user) => (user ? true : toLogin())));
  }

  if (auth.getRefreshToken()) {
    return auth.refresh().pipe(
      switchMap((res) => (res ? auth.reloadCurrentUser() : of(null))),
      map((user) => (user ? true : toLogin())),
    );
  }

  auth.clearSession();
  return toLogin();
};
