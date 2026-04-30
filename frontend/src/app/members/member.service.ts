import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, shareReplay } from 'rxjs';
import { environment } from '../../environments/environment';
import { Member, MemberRole, MemberRoleOption } from './member.model';

export interface MemberFilter {
  role?: MemberRole | null;
  activeOnly?: boolean;
  q?: string;
}

@Injectable({ providedIn: 'root' })
export class MemberService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/api/members`;
  private roles$?: Observable<MemberRoleOption[]>;

  list(filter: MemberFilter = {}): Observable<Member[]> {
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

  uploadPhoto(id: string, file: File): Observable<Member> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<Member>(`${this.baseUrl}/${id}/photo`, form);
  }

  roles(): Observable<MemberRoleOption[]> {
    if (!this.roles$) {
      this.roles$ = this.http
        .get<MemberRoleOption[]>(`${this.baseUrl}/roles`)
        .pipe(shareReplay({ bufferSize: 1, refCount: false }));
    }
    return this.roles$;
  }
}
