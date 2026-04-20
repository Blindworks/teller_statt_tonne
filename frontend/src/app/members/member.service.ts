import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Member, MemberType } from './member.model';

export interface MemberFilter {
  type?: MemberType | null;
  activeOnly?: boolean;
  q?: string;
}

@Injectable({ providedIn: 'root' })
export class MemberService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/api/members`;

  list(filter: MemberFilter = {}): Observable<Member[]> {
    let params = new HttpParams();
    if (filter.type) {
      params = params.set('type', filter.type);
    }
    if (filter.activeOnly) {
      params = params.set('activeOnly', 'true');
    }
    if (filter.q && filter.q.trim()) {
      params = params.set('q', filter.q.trim());
    }
    return this.http.get<Member[]>(this.baseUrl, { params });
  }

  get(id: string): Observable<Member> {
    return this.http.get<Member>(`${this.baseUrl}/${id}`);
  }

  create(member: Member): Observable<Member> {
    return this.http.post<Member>(this.baseUrl, member);
  }

  update(id: string, member: Member): Observable<Member> {
    return this.http.put<Member>(`${this.baseUrl}/${id}`, member);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
