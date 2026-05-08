import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { HygieneCertificate, HygieneCertificateStatus } from './hygiene-certificate.model';

@Injectable({ providedIn: 'root' })
export class HygieneCertificateService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/api`;

  getForUser(userId: number): Observable<HygieneCertificate | null> {
    return new Observable<HygieneCertificate | null>((subscriber) => {
      const sub = this.http
        .get<HygieneCertificate>(`${this.baseUrl}/users/${userId}/hygiene-certificate`)
        .subscribe({
          next: (cert) => {
            subscriber.next(cert);
            subscriber.complete();
          },
          error: (err) => {
            if (err?.status === 404) {
              subscriber.next(null);
              subscriber.complete();
            } else {
              subscriber.error(err);
            }
          },
        });
      return () => sub.unsubscribe();
    });
  }

  upload(userId: number, file: File, issuedDate: string): Observable<HygieneCertificate> {
    const form = new FormData();
    form.append('file', file);
    const params = new HttpParams().set('issuedDate', issuedDate);
    return this.http.post<HygieneCertificate>(
      `${this.baseUrl}/users/${userId}/hygiene-certificate`,
      form,
      { params },
    );
  }

  fetchFile(userId: number): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/users/${userId}/hygiene-certificate/file`, {
      responseType: 'blob',
    });
  }

  list(status?: HygieneCertificateStatus): Observable<HygieneCertificate[]> {
    const params = status ? { params: { status } } : {};
    return this.http.get<HygieneCertificate[]>(`${this.baseUrl}/hygiene-certificates`, params);
  }

  pendingCount(): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/hygiene-certificates/pending-count`);
  }

  approve(id: number): Observable<HygieneCertificate> {
    return this.http.post<HygieneCertificate>(
      `${this.baseUrl}/hygiene-certificates/${id}/approve`,
      null,
    );
  }

  reject(id: number, reason: string): Observable<HygieneCertificate> {
    return this.http.post<HygieneCertificate>(
      `${this.baseUrl}/hygiene-certificates/${id}/reject`,
      { reason },
    );
  }
}
