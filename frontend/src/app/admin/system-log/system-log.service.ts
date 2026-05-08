import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  SystemLogEventTypeMeta,
  SystemLogFilter,
  SystemLogPage,
} from './system-log.model';

@Injectable({ providedIn: 'root' })
export class SystemLogService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/api/admin/system-log`;

  list(filter: SystemLogFilter, page: number, size: number): Observable<SystemLogPage> {
    let params = new HttpParams().set('page', String(page)).set('size', String(size));
    if (filter.category) params = params.set('category', filter.category);
    if (filter.eventType) params = params.set('eventType', filter.eventType);
    if (filter.severity) params = params.set('severity', filter.severity);
    if (filter.actorUserId != null) params = params.set('actorUserId', String(filter.actorUserId));
    if (filter.from) params = params.set('from', filter.from);
    if (filter.to) params = params.set('to', filter.to);
    if (filter.search && filter.search.trim().length > 0) {
      params = params.set('search', filter.search.trim());
    }
    return this.http.get<SystemLogPage>(this.baseUrl, { params });
  }

  metadata(): Observable<SystemLogEventTypeMeta> {
    return this.http.get<SystemLogEventTypeMeta>(`${this.baseUrl}/event-types`);
  }
}
