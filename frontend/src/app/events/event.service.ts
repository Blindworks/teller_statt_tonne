import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { CharityEvent, EventScope } from './event.model';

@Injectable({ providedIn: 'root' })
export class EventService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/api/events`;

  list(scope: EventScope = 'active'): Observable<CharityEvent[]> {
    const params = new HttpParams().set('scope', scope);
    return this.http.get<CharityEvent[]>(this.baseUrl, { params });
  }

  get(id: number): Observable<CharityEvent> {
    return this.http.get<CharityEvent>(`${this.baseUrl}/${id}`);
  }

  create(payload: CharityEvent): Observable<CharityEvent> {
    return this.http.post<CharityEvent>(this.baseUrl, payload);
  }

  update(id: number, payload: CharityEvent): Observable<CharityEvent> {
    return this.http.put<CharityEvent>(`${this.baseUrl}/${id}`, payload);
  }

  remove(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  uploadLogo(id: number, file: File): Observable<CharityEvent> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<CharityEvent>(`${this.baseUrl}/${id}/logo`, form);
  }
}
