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
import { resolvePhotoUrl } from '../members/photo-url';

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
  readonly currentMember = this.auth.currentMember;

  readonly photoSrc = computed(() => resolvePhotoUrl(this.currentMember()?.photoUrl ?? null));
  readonly initials = computed(() => {
    const m = this.currentMember();
    const f = m?.firstName?.[0] ?? '';
    const l = m?.lastName?.[0] ?? '';
    const i = (f + l).trim().toUpperCase();
    if (i) return i;
    const email = this.currentUser()?.email ?? '';
    return email.slice(0, 1).toUpperCase() || '?';
  });
  readonly displayName = computed(() => {
    const m = this.currentMember();
    if (m) return `${m.firstName} ${m.lastName}`.trim();
    return this.currentUser()?.email ?? '';
  });

  readonly menuOpen = signal(false);

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
