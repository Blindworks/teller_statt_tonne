import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { UserAvailability } from './availability.model';

@Injectable({ providedIn: 'root' })
export class UserAvailabilityService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/api/users`;

  list(userId: number): Observable<UserAvailability[]> {
    return this.http.get<UserAvailability[]>(`${this.baseUrl}/${userId}/availabilities`);
  }

  replaceAll(userId: number, items: UserAvailability[]): Observable<UserAvailability[]> {
    return this.http.put<UserAvailability[]>(
      `${this.baseUrl}/${userId}/availabilities`,
      items,
    );
  }
}
