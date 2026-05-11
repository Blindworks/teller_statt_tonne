import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { DistributionPoint } from './distribution-point.model';

export interface GeocodingCoordinates {
  lat: number;
  lon: number;
}

@Injectable({ providedIn: 'root' })
export class DistributionPointService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/api/distribution-points`;
  private readonly geocodingUrl = `${environment.apiBaseUrl}/api/geocoding/forward`;

  forwardGeocode(
    street: string | null,
    postalCode: string | null,
    city: string | null,
  ): Observable<GeocodingCoordinates | null> {
    let params = new HttpParams();
    if (street && street.trim()) params = params.set('street', street.trim());
    if (postalCode && postalCode.trim()) params = params.set('postalCode', postalCode.trim());
    if (city && city.trim()) params = params.set('city', city.trim());
    return this.http.get<GeocodingCoordinates | null>(this.geocodingUrl, {
      params,
      observe: 'body',
    });
  }

  list(): Observable<DistributionPoint[]> {
    return this.http.get<DistributionPoint[]>(this.baseUrl);
  }

  get(id: number): Observable<DistributionPoint> {
    return this.http.get<DistributionPoint>(`${this.baseUrl}/${id}`);
  }

  create(payload: DistributionPoint): Observable<DistributionPoint> {
    return this.http.post<DistributionPoint>(this.baseUrl, payload);
  }

  update(id: number, payload: DistributionPoint): Observable<DistributionPoint> {
    return this.http.put<DistributionPoint>(`${this.baseUrl}/${id}`, payload);
  }

  remove(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
