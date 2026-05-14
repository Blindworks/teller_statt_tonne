import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface SystemSetting {
  key: string;
  value: string;
  updatedAt: string | null;
  updatedByUserId: number | null;
}

@Injectable({ providedIn: 'root' })
export class SystemSettingsService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/api/admin/settings`;

  list(): Observable<SystemSetting[]> {
    return this.http.get<SystemSetting[]>(this.baseUrl);
  }

  update(key: string, value: string): Observable<SystemSetting> {
    return this.http.put<SystemSetting>(`${this.baseUrl}/${encodeURIComponent(key)}`, { value });
  }
}
