import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Subject, debounceTime, distinctUntilChanged, switchMap } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { UserFilter, UserService } from '../user.service';
import { RoleName, RoleOption, User } from '../user.model';
import { PhotoUrlPipe } from '../photo-url.pipe';
import { UserProfileDialogService } from '../user-profile-dialog/user-profile-dialog.service';
import { AuthService } from '../../auth/auth.service';

type FilterChip = { label: string; role: RoleName | null; activeOnly: boolean };

@Component({
  selector: 'app-users-list',
  imports: [RouterLink, FormsModule, PhotoUrlPipe],
  templateUrl: './users-list.html',
  styleUrl: './users-list.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UsersListComponent {
  private readonly service = inject(UserService);
  private readonly profileDialog = inject(UserProfileDialogService);
  private readonly auth = inject(AuthService);
  private readonly reload$ = new Subject<UserFilter>();

  readonly canEditUsers = computed(() => {
    const roles = this.auth.currentUser()?.roles ?? [];
    return roles.includes('ADMINISTRATOR') || roles.includes('BOTSCHAFTER');
  });

  readonly users = signal<User[]>([]);
  readonly loadError = signal<string | null>(null);
  readonly activeFilterIndex = signal(0);
  readonly search = signal('');
  readonly roles = signal<RoleOption[]>([]);
  readonly filters = signal<FilterChip[]>([
    { label: 'Alle Nutzer', role: null, activeOnly: false },
    { label: 'Jetzt aktiv', role: null, activeOnly: true },
  ]);

  readonly totalCount = computed(() => this.users().length);

  constructor() {
    this.reload$
      .pipe(
        debounceTime(250),
        distinctUntilChanged(
          (a, b) => a.role === b.role && a.activeOnly === b.activeOnly && a.q === b.q,
        ),
        switchMap((filter) => this.service.list(filter)),
        takeUntilDestroyed(),
      )
      .subscribe({
        next: (list) => {
          this.users.set(list);
          this.loadError.set(null);
        },
        error: () => this.loadError.set('Nutzer konnten nicht geladen werden.'),
      });

    this.service
      .roles()
      .pipe(takeUntilDestroyed())
      .subscribe({
        next: (roles) => {
          this.roles.set(roles);
          const roleChips: FilterChip[] = roles.map((r) => ({
            label: r.label,
            role: r.value,
            activeOnly: false,
          }));
          this.filters.set([
            { label: 'Alle Nutzer', role: null, activeOnly: false },
            ...roleChips,
            { label: 'Jetzt aktiv', role: null, activeOnly: true },
          ]);
        },
      });

    this.triggerReload();
  }

  openProfile(userId: number | null): void {
    if (userId == null) return;
    this.profileDialog.open(userId);
  }

  setFilter(index: number): void {
    this.activeFilterIndex.set(index);
    this.triggerReload();
  }

  onSearchInput(value: string): void {
    this.search.set(value);
    this.triggerReload();
  }

  primaryRole(user: User): RoleName | null {
    return user.roles?.[0] ?? null;
  }

  roleBadgeClass(role: RoleName | null): string {
    switch (role) {
      case 'ADMINISTRATOR':
        return 'bg-error-container text-on-error-container';
      case 'BOTSCHAFTER':
        return 'bg-tertiary-container text-on-tertiary-fixed';
      case 'RETTER':
        return 'bg-primary-container text-on-primary-container';
      case 'NEW_MEMBER':
        return 'bg-secondary-container text-on-secondary-container';
      default:
        return 'bg-surface-container text-on-surface';
    }
  }

  roleLabel(role: RoleName | null): string {
    if (!role) return '—';
    return this.roles().find((r) => r.value === role)?.label ?? role;
  }

  onlineDotClass(status: User['onlineStatus']): string {
    switch (status) {
      case 'ONLINE':
        return 'bg-primary animate-pulse';
      case 'AWAY':
        return 'bg-on-surface-variant/30';
      case 'ON_BREAK':
        return 'bg-error';
      case 'OFFLINE':
        return 'bg-on-surface-variant/20';
    }
  }

  onlineLabel(status: User['onlineStatus']): string {
    switch (status) {
      case 'ONLINE':
        return 'Online';
      case 'AWAY':
        return 'Abwesend';
      case 'ON_BREAK':
        return 'Pause';
      case 'OFFLINE':
        return 'Offline';
    }
  }

  onlineTextClass(status: User['onlineStatus']): string {
    switch (status) {
      case 'ONLINE':
        return 'text-primary';
      case 'ON_BREAK':
        return 'text-error';
      default:
        return 'text-on-surface-variant';
    }
  }

  private triggerReload(): void {
    const chip = this.filters()[this.activeFilterIndex()];
    if (!chip) return;
    this.reload$.next({ role: chip.role, activeOnly: chip.activeOnly, q: this.search() });
  }
}
