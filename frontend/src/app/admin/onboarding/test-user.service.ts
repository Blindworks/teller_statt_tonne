import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface TestUser {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  status: string;
  createdAt: string;
}

export interface CreateTestUserRequest {
  firstName?: string;
  lastName?: string;
}

@Injectable({ providedIn: 'root' })
export class TestUserService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/api/admin/test-users`;

  list(): Observable<TestUser[]> {
    return this.http.get<TestUser[]>(this.baseUrl);
  }

  create(request?: CreateTestUserRequest): Observable<TestUser> {
    return this.http.post<TestUser>(this.baseUrl, request ?? {});
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
