import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, shareReplay } from 'rxjs';
import { environment } from '../../environments/environment';
import { Role, RoleOption, User } from './user.model';

export interface UserFilter {
  role?: Role | null;
  activeOnly?: boolean;
  q?: string;
}

export interface AdminCreateUserRequest {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  role: Role;
  phone?: string | null;
  city?: string | null;
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

  uploadPhoto(id: number, file: File): Observable<User> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<User>(`${this.baseUrl}/${id}/photo`, form);
  }

  roles(): Observable<RoleOption[]> {
    if (!this.roles$) {
      this.roles$ = this.http
        .get<RoleOption[]>(`${this.baseUrl}/roles`)
        .pipe(shareReplay({ bufferSize: 1, refCount: false }));
    }
    return this.roles$;
  }
}
