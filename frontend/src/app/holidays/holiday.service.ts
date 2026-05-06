import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface Holiday {
  date: string;
  name: string;
  region: 'DE' | 'NRW';
}

@Injectable({ providedIn: 'root' })
export class HolidayService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/api/holidays`;

  list(from: string, to: string): Observable<Holiday[]> {
    const params = new HttpParams().set('from', from).set('to', to);
    return this.http.get<Holiday[]>(this.baseUrl, { params });
  }
}
