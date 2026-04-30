import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { MemberAvailability } from './availability.model';

@Injectable({ providedIn: 'root' })
export class MemberAvailabilityService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/api/members`;

  list(memberId: string): Observable<MemberAvailability[]> {
    return this.http.get<MemberAvailability[]>(`${this.baseUrl}/${memberId}/availabilities`);
  }

  replaceAll(memberId: string, items: MemberAvailability[]): Observable<MemberAvailability[]> {
    return this.http.put<MemberAvailability[]>(
      `${this.baseUrl}/${memberId}/availabilities`,
      items,
    );
  }
}
