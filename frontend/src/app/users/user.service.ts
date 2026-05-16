import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, shareReplay } from 'rxjs';
import { environment } from '../../environments/environment';
import { RoleName, RoleOption, User } from './user.model';

export interface UserFilter {
  role?: RoleName | null;
  activeOnly?: boolean;
  q?: string;
}

export interface AdminCreateUserRequest {
  email: string;
  firstName: string;
  lastName: string;
  roleNames: RoleName[];
  phone?: string | null;
  street?: string | null;
  postalCode?: string | null;
  city?: string | null;
  country?: string | null;
}

@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/api/users`;
  private roles$?: Observable<RoleOption[]>;

  list(filter: UserFilter = {}): Observable<User[]> {
    let params = new HttpParams();
    if (filter.role) {
      params = params.set('role', filter.role);
    }
    if (filter.activeOnly) {
      params = params.set('activeOnly', 'true');
    }
    if (filter.q && filter.q.trim()) {
      params = params.set('q', filter.q.trim());
    }
    return this.http.get<User[]>(this.baseUrl, { params });
  }

  get(id: number): Observable<User> {
    return this.http.get<User>(`${this.baseUrl}/${id}`);
  }

  create(request: AdminCreateUserRequest): Observable<User> {
    return this.http.post<User>(this.baseUrl, request);
  }

  update(id: number, user: User): Observable<User> {
    return this.http.put<User>(`${this.baseUrl}/${id}`, user);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  resendInvitation(id: number): Observable<User> {
    return this.http.post<User>(`${this.baseUrl}/${id}/resend-invitation`, {});
  }

  markIntroductionCompleted(id: number): Observable<User> {
    return this.http.post<User>(`${this.baseUrl}/${id}/introduction-completed`, {});
  }

  pause(id: number): Observable<User> {
    return this.http.post<User>(`${this.baseUrl}/${id}/pause`, {});
  }

  reactivate(id: number): Observable<User> {
    return this.http.post<User>(`${this.baseUrl}/${id}/reactivate`, {});
  }

  forceActivate(id: number): Observable<User> {
    return this.http.post<User>(`${this.baseUrl}/${id}/force-activate`, {});
  }

  leave(id: number): Observable<User> {
    return this.http.post<User>(`${this.baseUrl}/${id}/leave`, {});
  }

  remove(id: number): Observable<User> {
    return this.http.post<User>(`${this.baseUrl}/${id}/remove`, {});
  }

  lock(id: number): Observable<User> {
    return this.http.post<User>(`${this.baseUrl}/${id}/lock`, {});
  }

  unlock(id: number): Observable<User> {
    return this.http.post<User>(`${this.baseUrl}/${id}/unlock`, {});
  }

  resetToOnboarding(id: number): Observable<User> {
    return this.http.post<User>(`${this.baseUrl}/${id}/reset-to-onboarding`, {});
  }

  uploadPhoto(id: number, file: File): Observable<User> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<User>(`${this.baseUrl}/${id}/photo`, form);
  }

  /**
   * Returns role options as `{value, label}` pairs.
   * Backend returns `{value: roleName, label: humanLabel}` from `GET /api/users/roles`.
   */
  roles(): Observable<RoleOption[]> {
    if (!this.roles$) {
      this.roles$ = this.http
        .get<RoleOption[]>(`${this.baseUrl}/roles`)
        .pipe(shareReplay({ bufferSize: 1, refCount: false }));
    }
    return this.roles$;
  }

  /** Reset cached role list (e.g. after admin creates/deletes a role). */
  invalidateRolesCache(): void {
    this.roles$ = undefined;
  }
}
