import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, catchError, finalize, of, shareReplay, tap, throwError } from 'rxjs';
import { environment } from '../../environments/environment';
import { AuthResponse, User } from './auth.models';
import { PermissionsService } from './permissions.service';

const ACCESS_KEY = 'tst.access';
const REFRESH_KEY = 'tst.refresh';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly permissions = inject(PermissionsService);
  private readonly baseUrl = `${environment.apiBaseUrl}/api/auth`;

  private readonly accessTokenSignal = signal<string | null>(this.readStorage(ACCESS_KEY));
  private readonly refreshTokenSignal = signal<string | null>(this.readStorage(REFRESH_KEY));
  private readonly currentUserSignal = signal<User | null>(null);
  private refreshInFlight: Observable<AuthResponse | null> | null = null;

  readonly currentUser = this.currentUserSignal.asReadonly();
  readonly isAuthenticated = computed(() => this.currentUserSignal() !== null);

  constructor() {
    if (this.accessTokenSignal()) {
      this.me().subscribe((user) => {
        if (user) {
          this.permissions.load().subscribe();
        }
      });
    }
  }

  getAccessToken(): string | null {
    return this.accessTokenSignal();
  }

  isAccessTokenExpired(): boolean {
    const token = this.accessTokenSignal();
    if (!token) return true;
    const exp = this.readJwtExp(token);
    if (exp === null) return false;
    return Date.now() / 1000 >= exp;
  }

  clearSession(): void {
    this.clearTokens();
  }

  private readJwtExp(token: string): number | null {
    const parts = token.split('.');
    if (parts.length !== 3) return null;
    try {
      const payload = JSON.parse(atob(parts[1].replace(/-/g, '+').replace(/_/g, '/')));
      return typeof payload.exp === 'number' ? payload.exp : null;
    } catch {
      return null;
    }
  }

  getRefreshToken(): string | null {
    return this.refreshTokenSignal();
  }

  login(email: string, password: string): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.baseUrl}/login`, { email, password })
      .pipe(tap((res) => this.handleAuthResponse(res)));
  }

  refresh(): Observable<AuthResponse | null> {
    if (this.refreshInFlight) {
      return this.refreshInFlight;
    }
    const refreshToken = this.refreshTokenSignal();
    if (!refreshToken) {
      return of(null);
    }
    this.refreshInFlight = this.http
      .post<AuthResponse>(`${this.baseUrl}/refresh`, { refreshToken })
      .pipe(
        tap((res) => this.handleAuthResponse(res)),
        catchError((err) => {
          if (err?.status === 401 || err?.status === 403) {
            this.clearTokens();
            return of(null);
          }
          return throwError(() => err);
        }),
        finalize(() => {
          this.refreshInFlight = null;
        }),
        shareReplay({ bufferSize: 1, refCount: false }),
      );
    return this.refreshInFlight;
  }

  me(): Observable<User | null> {
    return this.http.get<User>(`${this.baseUrl}/me`).pipe(
      tap((user) => this.currentUserSignal.set(user)),
      catchError((err) => {
        if (err?.status === 401 || err?.status === 403) {
          this.clearTokens();
        }
        return of(null);
      }),
    );
  }

  reloadCurrentUser(): Observable<User | null> {
    return this.me();
  }

  setCurrentUser(user: User | null): void {
    this.currentUserSignal.set(user);
  }

  changePassword(oldPassword: string, newPassword: string): Observable<void> {
    return this.http.put<void>(`${this.baseUrl}/password`, { oldPassword, newPassword });
  }

  forgotPassword(email: string): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/forgot-password`, { email });
  }

  resetPassword(token: string, newPassword: string): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/reset-password`, { token, newPassword });
  }

  logout(): Observable<void> {
    const refreshToken = this.refreshTokenSignal();
    this.clearTokens();
    if (!refreshToken) {
      return of(void 0);
    }
    return this.http.post<void>(`${this.baseUrl}/logout`, { refreshToken }).pipe(
      catchError(() => of(void 0)),
    );
  }

  private handleAuthResponse(res: AuthResponse): void {
    this.accessTokenSignal.set(res.accessToken);
    this.refreshTokenSignal.set(res.refreshToken);
    this.currentUserSignal.set(res.user);
    this.writeStorage(ACCESS_KEY, res.accessToken);
    this.writeStorage(REFRESH_KEY, res.refreshToken);
    this.permissions.clear();
    this.permissions.load().subscribe();
  }

  private clearTokens(): void {
    this.accessTokenSignal.set(null);
    this.refreshTokenSignal.set(null);
    this.currentUserSignal.set(null);
    this.writeStorage(ACCESS_KEY, null);
    this.writeStorage(REFRESH_KEY, null);
    this.permissions.clear();
  }

  private readStorage(key: string): string | null {
    if (typeof localStorage === 'undefined') return null;
    return localStorage.getItem(key);
  }

  private writeStorage(key: string, value: string | null): void {
    if (typeof localStorage === 'undefined') return;
    if (value === null) {
      localStorage.removeItem(key);
    } else {
      localStorage.setItem(key, value);
    }
  }
}
