import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  HostListener,
  computed,
  inject,
  signal,
} from '@angular/core';
import { Router } from '@angular/router';
import { NotificationService } from '../notification.service';
import { AppNotification } from '../notification.model';

@Component({
  selector: 'app-notification-bell',
  templateUrl: './notification-bell.html',
  styleUrl: './notification-bell.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NotificationBellComponent {
  private readonly notificationService = inject(NotificationService);
  private readonly router = inject(Router);
  private readonly host = inject(ElementRef<HTMLElement>);

  readonly open = signal(false);
  readonly notifications = this.notificationService.notifications;
  readonly unreadCount = this.notificationService.unreadCount;
  readonly visibleCount = computed(() => Math.min(10, this.notifications().length));
  readonly visibleNotifications = computed(() => this.notifications().slice(0, 10));
  readonly badgeText = computed(() => {
    const c = this.unreadCount();
    if (c <= 0) return null;
    return c > 99 ? '99+' : String(c);
  });

  toggle(event: MouseEvent): void {
    event.stopPropagation();
    const willOpen = !this.open();
    this.open.set(willOpen);
    if (willOpen) {
      this.notificationService.load().subscribe();
    }
  }

  close(): void {
    this.open.set(false);
  }

  onItemClick(item: AppNotification): void {
    if (item.readAt === null) {
      this.notificationService.markRead(item.id).subscribe();
    }
    this.open.set(false);
    if (item.relatedPickupId !== null) {
      this.router.navigate(['/pickups', item.relatedPickupId]);
    }
  }

  markAllRead(event: MouseEvent): void {
    event.stopPropagation();
    this.notificationService.markAllRead().subscribe();
  }

  formatRelative(iso: string): string {
    const then = new Date(iso).getTime();
    const now = Date.now();
    const diffSec = Math.floor((now - then) / 1000);
    if (diffSec < 60) return 'gerade eben';
    const diffMin = Math.floor(diffSec / 60);
    if (diffMin < 60) return `vor ${diffMin} Min.`;
    const diffH = Math.floor(diffMin / 60);
    if (diffH < 24) return `vor ${diffH} Std.`;
    const diffD = Math.floor(diffH / 24);
    if (diffD < 7) return `vor ${diffD} Tg.`;
    return new Date(iso).toLocaleDateString('de-DE');
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (!this.open()) return;
    if (!this.host.nativeElement.contains(event.target as Node)) {
      this.open.set(false);
    }
  }
}
