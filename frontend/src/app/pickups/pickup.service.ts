import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Pickup } from './pickup.model';

@Injectable({ providedIn: 'root' })
export class PickupService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/api/pickups`;

  list(from: string, to: string): Observable<Pickup[]> {
    const params = new HttpParams().set('from', from).set('to', to);
    return this.http.get<Pickup[]>(this.baseUrl, { params });
  }

  recent(): Observable<Pickup[]> {
    return this.http.get<Pickup[]>(`${this.baseUrl}/recent`);
  }

  upcoming(limit = 3): Observable<Pickup[]> {
    const params = new HttpParams().set('limit', String(limit));
    return this.http.get<Pickup[]>(`${this.baseUrl}/upcoming`, { params });
  }

  get(id: number): Observable<Pickup> {
    return this.http.get<Pickup>(`${this.baseUrl}/${id}`);
  }

  create(pickup: Pickup): Observable<Pickup> {
    return this.http.post<Pickup>(this.baseUrl, pickup);
  }

  update(id: number, pickup: Pickup): Observable<Pickup> {
    return this.http.put<Pickup>(`${this.baseUrl}/${id}`, pickup);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
