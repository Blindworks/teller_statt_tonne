import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { DaySlot } from './day-slot.model';

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/api`;

  day(date?: string): Observable<DaySlot[]> {
    let params = new HttpParams();
    if (date) params = params.set('date', date);
    return this.http.get<DaySlot[]>(`${this.baseUrl}/dashboard/day`, { params });
  }

  signup(pickupId: number): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/pickups/${pickupId}/signup`, {});
  }

  unassign(pickupId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/pickups/${pickupId}/signup`);
  }
}
