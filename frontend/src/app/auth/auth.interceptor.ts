import { HttpInterceptorFn, HttpRequest, HttpHandlerFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, switchMap, throwError } from 'rxjs';
import { environment } from '../../environments/environment';
import { AuthService } from './auth.service';

const AUTH_PATHS = ['/api/auth/login', '/api/auth/refresh', '/api/auth/logout'];

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (!req.url.startsWith(environment.apiBaseUrl)) {
    return next(req);
  }
  if (AUTH_PATHS.some((path) => req.url.startsWith(`${environment.apiBaseUrl}${path}`))) {
    return next(req);
  }

  return next(addToken(req, auth.getAccessToken())).pipe(
    catchError((err: HttpErrorResponse) => {
      if (err.status !== 401) {
        return throwError(() => err);
      }
      if (!auth.getRefreshToken()) {
        auth.clearSession();
        router.navigate(['/login'], { queryParams: { returnUrl: router.url } });
        return throwError(() => err);
      }
      return auth.refresh().pipe(
        switchMap((res) => {
          if (!res) {
            router.navigate(['/login'], { queryParams: { returnUrl: router.url } });
            return throwError(() => err);
          }
          return next(addToken(req, res.accessToken));
        }),
      );
    }),
  );
};

function addToken(req: HttpRequest<unknown>, token: string | null): HttpRequest<unknown> {
  if (!token) return req;
  return req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
}
