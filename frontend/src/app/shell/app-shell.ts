import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  HostListener,
  computed,
  effect,
  inject,
  signal,
} from '@angular/core';
import { NavigationStart, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../auth/auth.service';
import { NotificationBellComponent } from '../notifications/notification-bell/notification-bell';
import { NotificationService } from '../notifications/notification.service';
import { AppointmentService } from '../appointments/appointment.service';
import { hasAnyRole } from '../users/user.model';
import { resolvePhotoUrl } from '../users/photo-url';

@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, NotificationBellComponent],
  templateUrl: './app-shell.html',
  styleUrl: './app-shell.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AppShellComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly host = inject(ElementRef<HTMLElement>);
  private readonly notifications = inject(NotificationService);
  private readonly appointments = inject(AppointmentService);

  readonly currentUser = this.auth.currentUser;
  readonly appointmentsUnread = this.appointments.unreadCount;
  readonly bannerVisible = signal(false);
  readonly bannerCount = signal(0);
  readonly showShell = computed(() => this.auth.isAuthenticated());

  readonly photoSrc = computed(() => resolvePhotoUrl(this.currentUser()?.photoUrl ?? null));
  readonly initials = computed(() => {
    const u = this.currentUser();
    const f = u?.firstName?.[0] ?? '';
    const l = u?.lastName?.[0] ?? '';
    const i = (f + l).trim().toUpperCase();
    if (i) return i;
    const email = u?.email ?? '';
    return email.slice(0, 1).toUpperCase() || '?';
  });
  readonly displayName = computed(() => {
    const u = this.currentUser();
    if (u && (u.firstName || u.lastName)) return `${u.firstName} ${u.lastName}`.trim();
    return u?.email ?? '';
  });

  readonly menuOpen = signal(false);
  readonly moreOpen = signal(false);

  // Rollenbasierte Sichtbarkeit der Navigation.
  readonly isPlanner = computed(() =>
    hasAnyRole(this.currentUser(), 'ADMINISTRATOR', 'TEAMLEITER', 'KOORDINATOR', 'RETTER'),
  );
  readonly isAdmin = computed(() => hasAnyRole(this.currentUser(), 'ADMINISTRATOR'));
  readonly canSeeUsers = computed(() =>
    hasAnyRole(this.currentUser(), 'ADMINISTRATOR', 'TEAMLEITER', 'KOORDINATOR'),
  );
  readonly canSeeStoreMembers = computed(() =>
    hasAnyRole(this.currentUser(), 'ADMINISTRATOR', 'TEAMLEITER', 'KOORDINATOR'),
  );
  readonly canSeeDistributionPoints = computed(() =>
    hasAnyRole(this.currentUser(), 'ADMINISTRATOR', 'TEAMLEITER', 'KOORDINATOR'),
  );
  readonly canSeeTeamleitung = computed(() =>
    hasAnyRole(this.currentUser(), 'ADMINISTRATOR', 'TEAMLEITER', 'KOORDINATOR'),
  );
  readonly canSeeTickets = computed(() => this.auth.isAuthenticated());
  readonly canSeeRescuerCard = computed(() =>
    hasAnyRole(this.currentUser(), 'RETTER', 'ADMINISTRATOR', 'TEAMLEITER'),
  );

  constructor() {
    this.router.events.subscribe((e) => {
      if (e instanceof NavigationStart) {
        this.menuOpen.set(false);
        this.moreOpen.set(false);
      }
    });

    effect(() => {
      if (!this.auth.isAuthenticated() && !this.auth.getAccessToken()) {
        this.router.navigate(['/login']);
      }
    });

    effect(() => {
      if (this.auth.isAuthenticated()) {
        this.notifications.connect();
        this.notifications.load().subscribe();
        this.appointments.refreshUnreadCount().subscribe({
          next: ({ count }) => {
            if (count > 0 && !this.bannerVisible()) {
              this.bannerCount.set(count);
              this.bannerVisible.set(true);
            }
          },
        });
      } else {
        this.notifications.disconnect();
        this.appointments.resetUnread();
        this.bannerVisible.set(false);
      }
    });
  }

  dismissAppointmentsBanner(): void {
    this.bannerVisible.set(false);
  }

  toggleMenu(): void {
    this.menuOpen.update((v) => !v);
  }

  closeMenu(): void {
    this.menuOpen.set(false);
  }

  toggleMore(): void {
    this.moreOpen.update((v) => !v);
  }

  closeMore(): void {
    this.moreOpen.set(false);
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (!this.menuOpen() && !this.moreOpen()) return;
    const target = event.target as Node;
    if (!this.host.nativeElement.contains(target)) {
      this.menuOpen.set(false);
      this.moreOpen.set(false);
    }
  }

  logout(): void {
    this.menuOpen.set(false);
    this.auth.logout().subscribe(() => {
      this.router.navigateByUrl('/login');
    });
  }
}
