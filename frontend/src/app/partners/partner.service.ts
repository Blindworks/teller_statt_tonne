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

  list(): Observable<Partner[]> {
    return this.http.get<Partner[]>(this.baseUrl);
  }

  get(id: string): Observable<Partner> {
    return this.http.get<Partner>(`${this.baseUrl}/${id}`);
  }

  create(partner: Partner): Observable<Partner> {
    return this.http.post<Partner>(this.baseUrl, partner);
  }

  update(id: string, partner: Partner): Observable<Partner> {
    return this.http.put<Partner>(`${this.baseUrl}/${id}`, partner);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  regeocode(id: string): Observable<Partner> {
    return this.http.post<Partner>(`${this.baseUrl}/${id}/geocode`, null);
  }

  reverseGeocode(lat: number, lon: number): Observable<ReverseGeocodeResult | null> {
    return this.http.get<ReverseGeocodeResult | null>(
      `${environment.apiBaseUrl}/api/geocoding/reverse`,
      { params: { lat, lon } },
    );
  }
}
