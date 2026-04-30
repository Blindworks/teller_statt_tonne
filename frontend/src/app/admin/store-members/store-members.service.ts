import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { StoreMember } from './store-member.model';

@Injectable({ providedIn: 'root' })
export class StoreMembersService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/api/partners`;

  list(partnerId: string): Observable<StoreMember[]> {
    return this.http.get<StoreMember[]>(`${this.baseUrl}/${partnerId}/members`);
  }

  assign(partnerId: string, memberId: string): Observable<void> {
    return this.http.put<void>(`${this.baseUrl}/${partnerId}/members/${memberId}`, null);
  }

  unassign(partnerId: string, memberId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${partnerId}/members/${memberId}`);
  }
}
