import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Partner } from './partner.model';

export interface ReverseGeocodeResult {
  street: string | null;
  postalCode: string | null;
  city: string | null;
  lat: number;
  lon: number;
}

@Injectable({ providedIn: 'root' })
export class PartnerService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/api/partners`;

  list(mine = false): Observable<Partner[]> {
    const options = mine ? { params: { mine: 'true' } } : {};
    return this.http.get<Partner[]>(this.baseUrl, options);
  }

  listDeleted(): Observable<Partner[]> {
    return this.http.get<Partner[]>(`${this.baseUrl}/deleted`);
  }

  restore(id: number): Observable<Partner> {
    return this.http.post<Partner>(`${this.baseUrl}/${id}/restore`, null);
  }

  memberCounts(): Observable<Record<number, number>> {
    return this.http.get<Record<number, number>>(`${this.baseUrl}/member-counts`);
  }

  get(id: number): Observable<Partner> {
    return this.http.get<Partner>(`${this.baseUrl}/${id}`);
  }

  create(partner: Partner): Observable<Partner> {
    return this.http.post<Partner>(this.baseUrl, partner);
  }

  update(id: number, partner: Partner): Observable<Partner> {
    return this.http.put<Partner>(`${this.baseUrl}/${id}`, partner);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  uploadLogo(id: number, file: File): Observable<Partner> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<Partner>(`${this.baseUrl}/${id}/logo`, form);
  }

  regeocode(id: number): Observable<Partner> {
    return this.http.post<Partner>(`${this.baseUrl}/${id}/geocode`, null);
  }

  reverseGeocode(lat: number, lon: number): Observable<ReverseGeocodeResult | null> {
    return this.http.get<ReverseGeocodeResult | null>(
      `${environment.apiBaseUrl}/api/geocoding/reverse`,
      { params: { lat, lon } },
    );
  }
}
