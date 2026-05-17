import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { SpecialPickup, SpecialPickupScope } from './special-pickup.model';

@Injectable({ providedIn: 'root' })
export class SpecialPickupService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/api/special-pickups`;

  list(scope: SpecialPickupScope = 'active'): Observable<SpecialPickup[]> {
    const params = new HttpParams().set('scope', scope);
    return this.http.get<SpecialPickup[]>(this.baseUrl, { params });
  }

  get(id: number): Observable<SpecialPickup> {
    return this.http.get<SpecialPickup>(`${this.baseUrl}/${id}`);
  }

  create(payload: SpecialPickup): Observable<SpecialPickup> {
    return this.http.post<SpecialPickup>(this.baseUrl, payload);
  }

  update(id: number, payload: SpecialPickup): Observable<SpecialPickup> {
    return this.http.put<SpecialPickup>(`${this.baseUrl}/${id}`, payload);
  }

  remove(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  uploadLogo(id: number, file: File): Observable<SpecialPickup> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<SpecialPickup>(`${this.baseUrl}/${id}/logo`, form);
  }
}
