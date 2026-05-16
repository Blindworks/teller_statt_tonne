import { HttpInterceptorFn, HttpRequest, HttpHandlerFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, switchMap, throwError } from 'rxjs';
import { environment } from '../../environments/environment';
import { AuthService } from './auth.service';

const AUTH_PATHS = ['/api/auth/login', '/api/auth/refresh', '/api/auth/logout'];

// Oeffentliche Frontend-Routen, auf denen ein 401 aus Hintergrund-Requests
// (z.B. der App-Initializer fuer Partner-Kategorien) NICHT zu einem Login-
// Redirect fuehren darf. Sonst wuerde z.B. /reset-password/:token sofort auf
// /login?returnUrl=%2F umgeleitet, weil das Backend /api/partner-categories
// fuer anonyme User mit 401 ablehnt.
const PUBLIC_ROUTE_PREFIXES = [
  '/login',
  '/forgot-password',
  '/reset-password',
  '/about',
  '/impressum',
  '/datenschutz',
  '/quiz',
  '/verify',
];

function isOnPublicRoute(): boolean {
  const path = typeof window !== 'undefined' ? window.location.pathname : '';
  if (path === '/' || path === '') return true;
  return PUBLIC_ROUTE_PREFIXES.some((p) => path === p || path.startsWith(`${p}/`));
}

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
        if (!isOnPublicRoute()) {
          router.navigate(['/login'], { queryParams: { returnUrl: router.url } });
        }
        return throwError(() => err);
      }
      return auth.refresh().pipe(
        switchMap((res) => {
          if (!res) {
            if (!isOnPublicRoute()) {
              router.navigate(['/login'], { queryParams: { returnUrl: router.url } });
            }
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
