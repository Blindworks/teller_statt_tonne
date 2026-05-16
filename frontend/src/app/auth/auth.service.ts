import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import {
  Observable,
  catchError,
  finalize,
  firstValueFrom,
  of,
  shareReplay,
  tap,
  throwError,
} from 'rxjs';
import { environment } from '../../environments/environment';
import { AuthResponse, User } from './auth.models';

const ACCESS_KEY = 'tst.access';
const REFRESH_KEY = 'tst.refresh';
const ADMIN_BACKUP_ACCESS_KEY = 'tst.admin_access';
const ADMIN_BACKUP_REFRESH_KEY = 'tst.admin_refresh';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/api/auth`;

  private readonly accessTokenSignal = signal<string | null>(this.readStorage(ACCESS_KEY));
  private readonly refreshTokenSignal = signal<string | null>(this.readStorage(REFRESH_KEY));
  private readonly currentUserSignal = signal<User | null>(null);
  private readonly impersonatingSignal = signal<boolean>(this.readStorage(ADMIN_BACKUP_ACCESS_KEY) !== null);
  private refreshInFlight: Observable<AuthResponse | null> | null = null;
  private meInFlight: Promise<User | null> | null = null;

  readonly currentUser = this.currentUserSignal.asReadonly();
  readonly isAuthenticated = computed(() => this.currentUserSignal() !== null);
  readonly isImpersonating = this.impersonatingSignal.asReadonly();

  constructor() {
    if (this.accessTokenSignal()) {
      void this.ensureUserLoaded();
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
      .pipe(tap((res) => this.applySession(res)));
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
        tap((res) => this.applySession(res)),
        catchError((err) => {
          if (err?.status === 401 || err?.status === 403) {
            if (err?.error === 'Account locked') {
              try {
                sessionStorage.setItem('tst.lockReason', 'locked');
              } catch {
                /* sessionStorage unavailable — ignore */
              }
            }
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

  /**
   * Liefert den aktuellen User. Beim ersten Aufruf nach App-Start (Token vorhanden, User noch nicht geladen)
   * wartet die Methode auf die `/me`-Antwort. Folgende Aufrufe sind synchron via Signal.
   */
  ensureUserLoaded(): Promise<User | null> {
    const current = this.currentUserSignal();
    if (current) return Promise.resolve(current);
    if (!this.accessTokenSignal()) return Promise.resolve(null);
    if (this.meInFlight) return this.meInFlight;
    this.meInFlight = firstValueFrom(this.me()).finally(() => {
      this.meInFlight = null;
    });
    return this.meInFlight;
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

  impersonateTestUser(testUserId: number): Observable<AuthResponse> {
    const currentAccess = this.accessTokenSignal();
    const currentRefresh = this.refreshTokenSignal();
    if (!currentAccess || !currentRefresh) {
      return throwError(() => new Error('Kein aktiver Admin-Login.'));
    }
    return this.http
      .post<AuthResponse>(`${environment.apiBaseUrl}/api/admin/test-users/${testUserId}/impersonate`, {})
      .pipe(
        tap((res) => {
          this.writeStorage(ADMIN_BACKUP_ACCESS_KEY, currentAccess);
          this.writeStorage(ADMIN_BACKUP_REFRESH_KEY, currentRefresh);
          this.impersonatingSignal.set(true);
          this.applySession(res);
        }),
      );
  }

  endImpersonation(): boolean {
    const access = this.readStorage(ADMIN_BACKUP_ACCESS_KEY);
    const refresh = this.readStorage(ADMIN_BACKUP_REFRESH_KEY);
    if (!access || !refresh) {
      return false;
    }
    this.writeStorage(ADMIN_BACKUP_ACCESS_KEY, null);
    this.writeStorage(ADMIN_BACKUP_REFRESH_KEY, null);
    this.accessTokenSignal.set(access);
    this.refreshTokenSignal.set(refresh);
    this.writeStorage(ACCESS_KEY, access);
    this.writeStorage(REFRESH_KEY, refresh);
    this.impersonatingSignal.set(false);
    this.currentUserSignal.set(null);
    void this.ensureUserLoaded();
    return true;
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

  private applySession(res: AuthResponse): void {
    this.accessTokenSignal.set(res.accessToken);
    this.refreshTokenSignal.set(res.refreshToken);
    this.currentUserSignal.set(res.user);
    this.writeStorage(ACCESS_KEY, res.accessToken);
    this.writeStorage(REFRESH_KEY, res.refreshToken);
  }

  private clearTokens(): void {
    this.accessTokenSignal.set(null);
    this.refreshTokenSignal.set(null);
    this.currentUserSignal.set(null);
    this.writeStorage(ACCESS_KEY, null);
    this.writeStorage(REFRESH_KEY, null);
    this.writeStorage(ADMIN_BACKUP_ACCESS_KEY, null);
    this.writeStorage(ADMIN_BACKUP_REFRESH_KEY, null);
    this.impersonatingSignal.set(false);
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
