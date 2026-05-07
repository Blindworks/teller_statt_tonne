import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CreatePartnerNoteRequest, PartnerNote } from './partner-note.models';

@Injectable({ providedIn: 'root' })
export class PartnerNotesService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/api/partners`;

  list(partnerId: number): Observable<PartnerNote[]> {
    return this.http.get<PartnerNote[]>(`${this.baseUrl}/${partnerId}/notes`);
  }

  create(partnerId: number, request: CreatePartnerNoteRequest): Observable<PartnerNote> {
    return this.http.post<PartnerNote>(`${this.baseUrl}/${partnerId}/notes`, request);
  }

  delete(partnerId: number, noteId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${partnerId}/notes/${noteId}`);
  }
}
