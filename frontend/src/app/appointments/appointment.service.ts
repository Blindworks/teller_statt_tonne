import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { environment } from '../../environments/environment';
import { Appointment, AppointmentInput, PublicAppointment } from './appointment.model';

@Injectable({ providedIn: 'root' })
export class AppointmentService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/api/appointments`;
  private readonly publicUrl = `${environment.apiBaseUrl}/api/public/appointments`;

  private readonly unreadCountSignal = signal<number>(0);
  readonly unreadCount = this.unreadCountSignal.asReadonly();

  list(upcoming = true): Observable<Appointment[]> {
    const params = new HttpParams().set('upcoming', String(upcoming));
    return this.http.get<Appointment[]>(this.baseUrl, { params });
  }

  get(id: number): Observable<Appointment> {
    return this.http.get<Appointment>(`${this.baseUrl}/${id}`);
  }

  create(input: AppointmentInput): Observable<Appointment> {
    return this.http.post<Appointment>(this.baseUrl, input);
  }

  update(id: number, input: AppointmentInput): Observable<Appointment> {
    return this.http.put<Appointment>(`${this.baseUrl}/${id}`, input);
  }

  remove(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  refreshUnreadCount(): Observable<{ count: number }> {
    return this.http
      .get<{ count: number }>(`${this.baseUrl}/unread-count`)
      .pipe(tap((res) => this.unreadCountSignal.set(res.count)));
  }

  markRead(id: number): Observable<void> {
    return this.http
      .post<void>(`${this.baseUrl}/${id}/read`, {})
      .pipe(tap(() => this.refreshUnreadCount().subscribe()));
  }

  resetUnread(): void {
    this.unreadCountSignal.set(0);
  }

  listPublic(): Observable<PublicAppointment[]> {
    return this.http.get<PublicAppointment[]>(this.publicUrl);
  }
}
