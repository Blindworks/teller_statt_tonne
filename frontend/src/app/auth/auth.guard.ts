import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs';
import { AuthService } from './auth.service';

export const authGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (auth.isAuthenticated()) {
    return true;
  }

  const accessToken = auth.getAccessToken();
  if (accessToken && !auth.isAccessTokenExpired()) {
    return auth.reloadCurrentUser().pipe(
      map((user) =>
        user
          ? true
          : router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } }),
      ),
    );
  }

  auth.clearSession();
  return router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
};
