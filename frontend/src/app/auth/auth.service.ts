import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, catchError, of, switchMap, tap } from 'rxjs';
import { environment } from '../../environments/environment';
import { Member } from '../members/member.model';
import { AuthResponse, User } from './auth.models';

const ACCESS_KEY = 'tst.access';
const REFRESH_KEY = 'tst.refresh';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/api/auth`;

  private readonly accessTokenSignal = signal<string | null>(this.readStorage(ACCESS_KEY));
  private readonly refreshTokenSignal = signal<string | null>(this.readStorage(REFRESH_KEY));
  private readonly currentUserSignal = signal<User | null>(null);
  private readonly currentMemberSignal = signal<Member | null>(null);

  readonly currentUser = this.currentUserSignal.asReadonly();
  readonly currentMember = this.currentMemberSignal.asReadonly();
  readonly isAuthenticated = computed(() => this.currentUserSignal() !== null);

  constructor() {
    if (this.accessTokenSignal()) {
      this.me().subscribe();
    }
  }

  getAccessToken(): string | null {
    return this.accessTokenSignal();
  }

  getRefreshToken(): string | null {
    return this.refreshTokenSignal();
  }

  login(email: string, password: string): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.baseUrl}/login`, { email, password })
      .pipe(tap((res) => this.handleAuthResponse(res)));
  }

  register(email: string, password: string): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.baseUrl}/register`, { email, password })
      .pipe(tap((res) => this.handleAuthResponse(res)));
  }

  refresh(): Observable<AuthResponse | null> {
    const refreshToken = this.refreshTokenSignal();
    if (!refreshToken) {
      return of(null);
    }
    return this.http
      .post<AuthResponse>(`${this.baseUrl}/refresh`, { refreshToken })
      .pipe(
        tap((res) => this.handleAuthResponse(res)),
        catchError(() => {
          this.clearTokens();
          return of(null);
        }),
      );
  }

  me(): Observable<User | null> {
    return this.http.get<User>(`${this.baseUrl}/me`).pipe(
      tap((user) => this.currentUserSignal.set(user)),
      switchMap((user) => this.loadMember(user).pipe(switchMap(() => of(user)))),
      catchError(() => {
        this.currentUserSignal.set(null);
        this.currentMemberSignal.set(null);
        return of(null);
      }),
    );
  }

  reloadCurrentMember(): Observable<Member | null> {
    return this.loadMember(this.currentUserSignal());
  }

  setCurrentMember(member: Member | null): void {
    this.currentMemberSignal.set(member);
  }

  changePassword(oldPassword: string, newPassword: string): Observable<void> {
    return this.http.put<void>(`${this.baseUrl}/password`, { oldPassword, newPassword });
  }

  private loadMember(user: User | null): Observable<Member | null> {
    if (!user || !user.memberId) {
      this.currentMemberSignal.set(null);
      return of(null);
    }
    return this.http.get<Member>(`${environment.apiBaseUrl}/api/members/${user.memberId}`).pipe(
      tap((m) => this.currentMemberSignal.set(m)),
      catchError(() => {
        this.currentMemberSignal.set(null);
        return of(null);
      }),
    );
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
    this.loadMember(res.user).subscribe();
  }

  private clearTokens(): void {
    this.accessTokenSignal.set(null);
    this.refreshTokenSignal.set(null);
    this.currentUserSignal.set(null);
    this.currentMemberSignal.set(null);
    this.writeStorage(ACCESS_KEY, null);
    this.writeStorage(REFRESH_KEY, null);
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
