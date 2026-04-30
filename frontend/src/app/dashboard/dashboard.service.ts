import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { DaySlot } from './day-slot.model';

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/api/dashboard`;

  day(date?: string): Observable<DaySlot[]> {
    let params = new HttpParams();
    if (date) params = params.set('date', date);
    return this.http.get<DaySlot[]>(`${this.baseUrl}/day`, { params });
  }
}
