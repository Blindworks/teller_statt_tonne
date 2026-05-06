import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { UserService } from '../../users/user.service';
import { Role, RoleCreateRequest, RoleUpdateRequest } from './role.model';

@Injectable({ providedIn: 'root' })
export class RoleService {
  private readonly http = inject(HttpClient);
  private readonly userService = inject(UserService);
  private readonly baseUrl = `${environment.apiBaseUrl}/api/roles`;

  list(includeDisabled = true): Observable<Role[]> {
    const params = includeDisabled ? '?includeDisabled=true' : '';
    return this.http.get<Role[]>(`${this.baseUrl}${params}`);
  }

  get(id: number): Observable<Role> {
    return this.http.get<Role>(`${this.baseUrl}/${id}`);
  }

  create(req: RoleCreateRequest): Observable<Role> {
    return this.http
      .post<Role>(this.baseUrl, req)
      .pipe(tap(() => this.userService.invalidateRolesCache()));
  }

  update(id: number, req: RoleUpdateRequest): Observable<Role> {
    return this.http
      .put<Role>(`${this.baseUrl}/${id}`, req)
      .pipe(tap(() => this.userService.invalidateRolesCache()));
  }

  remove(id: number): Observable<void> {
    return this.http
      .delete<void>(`${this.baseUrl}/${id}`)
      .pipe(tap(() => this.userService.invalidateRolesCache()));
  }
}
