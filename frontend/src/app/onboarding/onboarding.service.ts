import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { IntroductionSlot, IntroductionSlotRequest, OnboardingStatus } from './onboarding.models';
import { User } from '../users/user.model';

@Injectable({ providedIn: 'root' })
export class OnboardingService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/api`;

  getMyStatus(): Observable<OnboardingStatus> {
    return this.http.get<OnboardingStatus>(`${this.baseUrl}/onboarding/me`);
  }

  getUserStatus(userId: number): Observable<OnboardingStatus> {
    return this.http.get<OnboardingStatus>(`${this.baseUrl}/onboarding/users/${userId}`);
  }

  uploadAgreement(userId: number, file: File): Observable<OnboardingStatus> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<OnboardingStatus>(`${this.baseUrl}/users/${userId}/agreement`, form);
  }

  setTestPickup(userId: number, completed: boolean): Observable<OnboardingStatus> {
    return this.http.patch<OnboardingStatus>(`${this.baseUrl}/users/${userId}/test-pickup`, {
      completed,
    });
  }

  updateSelfProfile(
    userId: number,
    profile: { phone: string; street: string; postalCode: string; city: string; country: string },
  ): Observable<User> {
    return this.http.put<User>(`${this.baseUrl}/users/${userId}/self-profile`, profile);
  }

  availableSlots(): Observable<IntroductionSlot[]> {
    return this.http.get<IntroductionSlot[]>(`${this.baseUrl}/introduction-slots/available`);
  }

  listAllSlots(): Observable<IntroductionSlot[]> {
    return this.http.get<IntroductionSlot[]>(`${this.baseUrl}/introduction-slots`);
  }

  createSlot(req: IntroductionSlotRequest): Observable<IntroductionSlot> {
    return this.http.post<IntroductionSlot>(`${this.baseUrl}/introduction-slots`, req);
  }

  updateSlot(id: number, req: IntroductionSlotRequest): Observable<IntroductionSlot> {
    return this.http.patch<IntroductionSlot>(`${this.baseUrl}/introduction-slots/${id}`, req);
  }

  deleteSlot(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/introduction-slots/${id}`);
  }

  bookSlot(id: number): Observable<IntroductionSlot> {
    return this.http.post<IntroductionSlot>(
      `${this.baseUrl}/introduction-slots/${id}/book`,
      null,
    );
  }

  cancelBooking(bookingId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/introduction-slots/bookings/${bookingId}`);
  }

  confirmAttendance(slotId: number, userId: number): Observable<void> {
    return this.http.post<void>(
      `${this.baseUrl}/introduction-slots/${slotId}/confirm-attendance/${userId}`,
      null,
    );
  }
}
