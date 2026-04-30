import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  HostListener,
  computed,
  inject,
  signal,
} from '@angular/core';
import { NavigationStart, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../auth/auth.service';
import { resolvePhotoUrl } from '../users/photo-url';

@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './app-shell.html',
  styleUrl: './app-shell.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AppShellComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly host = inject(ElementRef<HTMLElement>);

  readonly currentUser = this.auth.currentUser;

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

  readonly isPlanner = computed(() => {
    const role = this.currentUser()?.role;
    return role === 'ADMINISTRATOR' || role === 'BOTSCHAFTER';
  });

  constructor() {
    this.router.events.subscribe((e) => {
      if (e instanceof NavigationStart) this.menuOpen.set(false);
    });
  }

  toggleMenu(): void {
    this.menuOpen.update((v) => !v);
  }

  closeMenu(): void {
    this.menuOpen.set(false);
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (!this.menuOpen()) return;
    const target = event.target as Node;
    if (!this.host.nativeElement.contains(target)) {
      this.menuOpen.set(false);
    }
  }

  logout(): void {
    this.menuOpen.set(false);
    this.auth.logout().subscribe(() => {
      this.router.navigateByUrl('/login');
    });
  }
}
