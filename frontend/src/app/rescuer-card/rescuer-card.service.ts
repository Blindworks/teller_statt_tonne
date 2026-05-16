import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  RescuerCardContext,
  RescuerCardTokenResponse,
  VerifyRescuerResponse,
} from './rescuer-card.model';

@Injectable({ providedIn: 'root' })
export class RescuerCardService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/api/rescuer-card`;
  private readonly publicBase = `${environment.apiBaseUrl}/api/public/verify`;

  getContext(): Observable<RescuerCardContext> {
    return this.http.get<RescuerCardContext>(`${this.baseUrl}/context`);
  }

  issueToken(): Observable<RescuerCardTokenResponse> {
    return this.http.get<RescuerCardTokenResponse>(`${this.baseUrl}/token`);
  }

  verify(token: string): Observable<VerifyRescuerResponse> {
    return this.http.get<VerifyRescuerResponse>(`${this.publicBase}/${encodeURIComponent(token)}`);
  }
}
