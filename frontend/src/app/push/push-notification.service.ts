import { HttpClient } from '@angular/common/http';
import { DestroyRef, Injectable, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router } from '@angular/router';
import { SwPush } from '@angular/service-worker';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';

interface BackendSubscription {
  endpoint: string;
  p256dh: string;
  auth: string;
  userAgent?: string | null;
}

@Injectable({ providedIn: 'root' })
export class PushNotificationService {
  private readonly swPush = inject(SwPush);
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  private readonly currentSubscription = signal<PushSubscription | null>(null);
  private readonly permission = signal<NotificationPermission>(this.readPermission());

  readonly isSupported = this.swPush.isEnabled && typeof Notification !== 'undefined';
  readonly permissionState = computed(() => this.permission());
  readonly isSubscribed = computed(() => this.currentSubscription() !== null);

  constructor() {
    if (!this.isSupported) return;

    this.swPush.subscription.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((sub) => {
      this.currentSubscription.set(sub);
    });

    this.swPush.notificationClicks
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((event) => {
        const url = (event.notification.data as { url?: string } | undefined)?.url;
        if (url) {
          this.router.navigateByUrl(url);
        }
      });
  }

  async subscribe(): Promise<void> {
    if (!this.isSupported) {
      throw new Error('Push-Benachrichtigungen werden auf diesem Gerät nicht unterstützt.');
    }
    const serverPublicKey = await this.resolvePublicKey();
    if (!serverPublicKey) {
      throw new Error('VAPID Public Key fehlt. Bitte in der Server-Konfiguration setzen.');
    }
    const sub = await this.swPush.requestSubscription({ serverPublicKey });
    this.permission.set(this.readPermission());
    await firstValueFrom(
      this.http.post<void>(
        `${environment.apiBaseUrl}/api/push/subscriptions`,
        this.toBackendDto(sub),
      ),
    );
  }

  async unsubscribe(): Promise<void> {
    const sub = this.currentSubscription();
    if (!sub) return;
    const endpoint = sub.endpoint;
    try {
      await this.swPush.unsubscribe();
    } catch {
      /* noop — wir versuchen trotzdem, die Subscription serverseitig aufzuräumen. */
    }
    await firstValueFrom(
      this.http.request<void>(
        'DELETE',
        `${environment.apiBaseUrl}/api/push/subscriptions`,
        { body: { endpoint } },
      ),
    );
  }

  private async resolvePublicKey(): Promise<string | null> {
    if (environment.vapidPublicKey) return environment.vapidPublicKey;
    try {
      const key = await firstValueFrom(
        this.http.get(`${environment.apiBaseUrl}/api/push/vapid-public-key`, {
          responseType: 'text',
        }),
      );
      return key?.trim() || null;
    } catch {
      return null;
    }
  }

  private toBackendDto(sub: PushSubscription): BackendSubscription {
    const json = sub.toJSON();
    const keys = (json.keys ?? {}) as Record<string, string>;
    return {
      endpoint: sub.endpoint,
      p256dh: keys['p256dh'],
      auth: keys['auth'],
      userAgent: typeof navigator !== 'undefined' ? navigator.userAgent : null,
    };
  }

  private readPermission(): NotificationPermission {
    if (typeof Notification === 'undefined') return 'denied';
    return Notification.permission;
  }
}
