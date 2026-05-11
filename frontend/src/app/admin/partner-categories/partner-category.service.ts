import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PartnerCategory } from './partner-category.model';

@Injectable({ providedIn: 'root' })
export class PartnerCategoryService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/api/partner-categories`;

  listActive(): Observable<PartnerCategory[]> {
    return this.http.get<PartnerCategory[]>(this.baseUrl);
  }

  listAll(): Observable<PartnerCategory[]> {
    return this.http.get<PartnerCategory[]>(`${this.baseUrl}/all`);
  }

  get(id: number): Observable<PartnerCategory> {
    return this.http.get<PartnerCategory>(`${this.baseUrl}/${id}`);
  }

  create(payload: PartnerCategory): Observable<PartnerCategory> {
    return this.http.post<PartnerCategory>(this.baseUrl, payload);
  }

  update(id: number, payload: PartnerCategory): Observable<PartnerCategory> {
    return this.http.put<PartnerCategory>(`${this.baseUrl}/${id}`, payload);
  }

  remove(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
