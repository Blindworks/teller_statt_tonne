import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { ApplicationStatus, PartnerApplication } from './partner-application.model';

@Injectable({ providedIn: 'root' })
export class PartnerApplicationsService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/api`;

  apply(partnerId: number, message: string | null): Observable<PartnerApplication> {
    return this.http.post<PartnerApplication>(`${this.baseUrl}/partner-applications`, {
      partnerId,
      message,
    });
  }

  withdraw(applicationId: number): Observable<PartnerApplication> {
    return this.http.delete<PartnerApplication>(
      `${this.baseUrl}/partner-applications/${applicationId}`,
    );
  }

  approve(applicationId: number): Observable<PartnerApplication> {
    return this.http.post<PartnerApplication>(
      `${this.baseUrl}/partner-applications/${applicationId}/approve`,
      null,
    );
  }

  reject(applicationId: number, reason: string | null): Observable<PartnerApplication> {
    return this.http.post<PartnerApplication>(
      `${this.baseUrl}/partner-applications/${applicationId}/reject`,
      { reason },
    );
  }

  listForPartner(
    partnerId: number,
    status?: ApplicationStatus,
  ): Observable<PartnerApplication[]> {
    const params = status ? { params: { status } } : {};
    return this.http.get<PartnerApplication[]>(
      `${this.baseUrl}/partners/${partnerId}/applications`,
      params,
    );
  }

  listMine(): Observable<PartnerApplication[]> {
    return this.http.get<PartnerApplication[]>(`${this.baseUrl}/users/me/partner-applications`);
  }

  pendingCount(): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/partner-applications/pending-count`);
  }
}
