import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  Ticket,
  TicketCategory,
  TicketComment,
  TicketCreateRequest,
  TicketStatus,
  TicketSummary,
  TicketUpdateRequest,
} from './ticket.model';

@Injectable({ providedIn: 'root' })
export class TicketService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/api/tickets`;

  list(filter?: { status?: TicketStatus; category?: TicketCategory }): Observable<TicketSummary[]> {
    let params = new HttpParams();
    if (filter?.status) params = params.set('status', filter.status);
    if (filter?.category) params = params.set('category', filter.category);
    return this.http.get<TicketSummary[]>(this.baseUrl, { params });
  }

  get(id: number): Observable<Ticket> {
    return this.http.get<Ticket>(`${this.baseUrl}/${id}`);
  }

  create(payload: TicketCreateRequest): Observable<Ticket> {
    return this.http.post<Ticket>(this.baseUrl, payload);
  }

  update(id: number, payload: TicketUpdateRequest): Observable<Ticket> {
    return this.http.put<Ticket>(`${this.baseUrl}/${id}`, payload);
  }

  updateStatus(id: number, status: TicketStatus): Observable<Ticket> {
    return this.http.patch<Ticket>(`${this.baseUrl}/${id}/status`, { status });
  }

  remove(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  uploadAttachment(id: number, file: File): Observable<Ticket> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<Ticket>(`${this.baseUrl}/${id}/attachments`, form);
  }

  deleteAttachment(ticketId: number, attachmentId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${ticketId}/attachments/${attachmentId}`);
  }

  addComment(ticketId: number, body: string): Observable<TicketComment> {
    return this.http.post<TicketComment>(`${this.baseUrl}/${ticketId}/comments`, { body });
  }

  deleteComment(ticketId: number, commentId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${ticketId}/comments/${commentId}`);
  }
}
