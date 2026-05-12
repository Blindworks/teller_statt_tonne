import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  CompleteResponse,
  DistributionPost,
  PickupRun,
  PickupRunItem,
} from './pickup-run.model';

@Injectable({ providedIn: 'root' })
export class PickupRunService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/api/pickups`;
  private readonly postsUrl = `${environment.apiBaseUrl}/api/distribution-posts`;

  start(pickupId: number): Observable<PickupRun> {
    return this.http.post<PickupRun>(`${this.baseUrl}/${pickupId}/run/start`, {});
  }

  current(pickupId: number): Observable<PickupRun> {
    return this.http.get<PickupRun>(`${this.baseUrl}/${pickupId}/run`);
  }

  addItem(
    pickupId: number,
    body: { foodCategoryId?: number; customLabel?: string },
  ): Observable<PickupRunItem> {
    return this.http.post<PickupRunItem>(`${this.baseUrl}/${pickupId}/run/items`, body);
  }

  removeItem(pickupId: number, itemId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${pickupId}/run/items/${itemId}`);
  }

  complete(
    pickupId: number,
    body: { distributionPointId: number; notes?: string | null },
  ): Observable<CompleteResponse> {
    return this.http.post<CompleteResponse>(`${this.baseUrl}/${pickupId}/run/complete`, body);
  }

  abort(pickupId: number): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/${pickupId}/run/abort`, {});
  }

  addPhoto(postId: number, file: File): Observable<DistributionPost> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<DistributionPost>(`${this.postsUrl}/${postId}/photos`, form);
  }
}
