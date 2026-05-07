import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { environment } from '../../environments/environment';
import { AuthService } from '../auth/auth.service';
import { AppNotification } from './notification.model';

const RECONNECT_DELAYS_MS = [1_000, 2_000, 5_000, 10_000, 30_000];

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);
  private readonly baseUrl = `${environment.apiBaseUrl}/api/notifications`;

  private readonly notificationsSignal = signal<AppNotification[]>([]);
  private readonly unreadCountSignal = signal<number>(0);

  readonly notifications = this.notificationsSignal.asReadonly();
  readonly unreadCount = this.unreadCountSignal.asReadonly();
  readonly hasUnread = computed(() => this.unreadCountSignal() > 0);

  private abortController: AbortController | null = null;
  private reconnectAttempt = 0;
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null;
  private connected = false;

  load(): Observable<AppNotification[]> {
    return this.http.get<AppNotification[]>(this.baseUrl).pipe(
      tap((items) => {
        this.notificationsSignal.set(items);
        this.unreadCountSignal.set(items.filter((n) => n.readAt === null).length);
      }),
    );
  }

  markRead(id: number): Observable<void> {
    return this.http.patch<void>(`${this.baseUrl}/${id}/read`, {}).pipe(
      tap(() => {
        const now = new Date().toISOString();
        this.notificationsSignal.update((list) =>
          list.map((n) => (n.id === id && n.readAt === null ? { ...n, readAt: now } : n)),
        );
        this.unreadCountSignal.update((c) => Math.max(0, c - 1));
      }),
    );
  }

  markAllRead(): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/read-all`, {}).pipe(
      tap(() => {
        const now = new Date().toISOString();
        this.notificationsSignal.update((list) =>
          list.map((n) => (n.readAt === null ? { ...n, readAt: now } : n)),
        );
        this.unreadCountSignal.set(0);
      }),
    );
  }

  connect(): void {
    if (this.connected) return;
    this.connected = true;
    this.openStream();
  }

  disconnect(): void {
    this.connected = false;
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }
    if (this.abortController) {
      this.abortController.abort();
      this.abortController = null;
    }
    this.notificationsSignal.set([]);
    this.unreadCountSignal.set(0);
  }

  private async openStream(): Promise<void> {
    const token = this.auth.getAccessToken();
    if (!token) {
      this.scheduleReconnect();
      return;
    }
    const controller = new AbortController();
    this.abortController = controller;
    try {
      const response = await fetch(`${this.baseUrl}/stream`, {
        method: 'GET',
        headers: { Authorization: `Bearer ${token}`, Accept: 'text/event-stream' },
        signal: controller.signal,
        credentials: 'include',
      });
      if (!response.ok || !response.body) {
        throw new Error(`SSE response status ${response.status}`);
      }
      this.reconnectAttempt = 0;
      const reader = response.body.getReader();
      const decoder = new TextDecoder('utf-8');
      let buffer = '';
      while (this.connected) {
        const { value, done } = await reader.read();
        if (done) break;
        buffer += decoder.decode(value, { stream: true });
        let separatorIndex: number;
        while ((separatorIndex = buffer.indexOf('\n\n')) >= 0) {
          const rawEvent = buffer.slice(0, separatorIndex);
          buffer = buffer.slice(separatorIndex + 2);
          this.handleSseEvent(rawEvent);
        }
      }
    } catch (err) {
      if (controller.signal.aborted) return;
    } finally {
      this.abortController = null;
    }
    if (this.connected) {
      this.scheduleReconnect();
    }
  }

  private handleSseEvent(raw: string): void {
    const lines = raw.split('\n');
    let eventName = 'message';
    const dataLines: string[] = [];
    for (const line of lines) {
      if (line.startsWith('event:')) {
        eventName = line.slice(6).trim();
      } else if (line.startsWith('data:')) {
        dataLines.push(line.slice(5).trimStart());
      }
    }
    const dataStr = dataLines.join('\n');
    if (eventName === 'notification') {
      try {
        const dto = JSON.parse(dataStr) as AppNotification;
        this.notificationsSignal.update((list) => [dto, ...list].slice(0, 100));
        if (dto.readAt === null) {
          this.unreadCountSignal.update((c) => c + 1);
        }
      } catch {
        /* ignore malformed payload */
      }
    } else if (eventName === 'unread-count') {
      try {
        const payload = JSON.parse(dataStr) as { count: number };
        this.unreadCountSignal.set(payload.count);
      } catch {
        /* ignore */
      }
    }
  }

  private scheduleReconnect(): void {
    if (!this.connected) return;
    const delay = RECONNECT_DELAYS_MS[Math.min(this.reconnectAttempt, RECONNECT_DELAYS_MS.length - 1)];
    this.reconnectAttempt++;
    this.reconnectTimer = setTimeout(() => {
      this.reconnectTimer = null;
      this.openStream();
    }, delay);
  }
}
