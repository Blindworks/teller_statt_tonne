import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Feature, FeatureRequest, RoleFeatureAssignment } from './feature.model';

@Injectable({ providedIn: 'root' })
export class FeatureService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/api/features`;
  private readonly rolesUrl = `${environment.apiBaseUrl}/api/roles`;

  list(): Observable<Feature[]> {
    return this.http.get<Feature[]>(this.baseUrl);
  }

  create(req: FeatureRequest): Observable<Feature> {
    return this.http.post<Feature>(this.baseUrl, req);
  }

  update(id: number, req: FeatureRequest): Observable<Feature> {
    return this.http.put<Feature>(`${this.baseUrl}/${id}`, req);
  }

  remove(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  getForRole(roleId: number): Observable<RoleFeatureAssignment> {
    return this.http.get<RoleFeatureAssignment>(`${this.rolesUrl}/${roleId}/features`);
  }

  setForRole(roleId: number, featureIds: number[]): Observable<RoleFeatureAssignment> {
    return this.http.put<RoleFeatureAssignment>(`${this.rolesUrl}/${roleId}/features`, {
      featureIds,
    });
  }
}
