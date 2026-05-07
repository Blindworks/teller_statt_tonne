import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface StatsPartnerEntry {
  partnerId: number;
  partnerName: string | null;
  savedKg: number;
  pickupCount: number;
}

export interface StatsMemberEntry {
  memberId: number;
  memberName: string | null;
  savedKg: number;
  pickupCount: number;
}

export interface StatsOverview {
  totalSavedKg: number;
  completedPickupCount: number;
  topPartners: StatsPartnerEntry[];
  topMembers: StatsMemberEntry[];
}

@Injectable({ providedIn: 'root' })
export class StatsService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/api/stats`;

  overview(): Observable<StatsOverview> {
    return this.http.get<StatsOverview>(`${this.baseUrl}/overview`);
  }
}
