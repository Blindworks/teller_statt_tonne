import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Subject, debounceTime, distinctUntilChanged, switchMap } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MemberFilter, MemberService } from '../member.service';
import { Member, MemberRole, MemberRoleOption } from '../member.model';
import { PhotoUrlPipe } from '../photo-url.pipe';

type FilterChip = { label: string; role: MemberRole | null; activeOnly: boolean };

@Component({
  selector: 'app-members-list',
  imports: [RouterLink, FormsModule, PhotoUrlPipe],
  templateUrl: './members-list.html',
  styleUrl: './members-list.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MembersListComponent {
  private readonly service = inject(MemberService);
  private readonly reload$ = new Subject<MemberFilter>();

  readonly members = signal<Member[]>([]);
  readonly loadError = signal<string | null>(null);
  readonly activeFilterIndex = signal(0);
  readonly search = signal('');
  readonly roles = signal<MemberRoleOption[]>([]);
  readonly filters = signal<FilterChip[]>([
    { label: 'Alle Mitglieder', role: null, activeOnly: false },
    { label: 'Jetzt aktiv', role: null, activeOnly: true },
  ]);

  readonly totalCount = computed(() => this.members().length);

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
          this.members.set(list);
          this.loadError.set(null);
        },
        error: () => this.loadError.set('Mitglieder konnten nicht geladen werden.'),
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
            { label: 'Alle Mitglieder', role: null, activeOnly: false },
            ...roleChips,
            { label: 'Jetzt aktiv', role: null, activeOnly: true },
          ]);
        },
      });

    this.triggerReload();
  }

  setFilter(index: number): void {
    this.activeFilterIndex.set(index);
    this.triggerReload();
  }

  onSearchInput(value: string): void {
    this.search.set(value);
    this.triggerReload();
  }

  roleBadgeClass(role: MemberRole): string {
    switch (role) {
      case 'BOTSCHAFTER':
        return 'bg-tertiary-container text-on-tertiary-fixed';
      case 'FOODSAVER':
        return 'bg-primary-container text-on-primary-container';
      case 'NEW_MEMBER':
        return 'bg-secondary-container text-on-secondary-container';
      default:
        return 'bg-surface-container text-on-surface';
    }
  }

  roleLabel(role: MemberRole): string {
    return this.roles().find((r) => r.value === role)?.label ?? role;
  }

  onlineDotClass(status: Member['onlineStatus']): string {
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

  onlineLabel(status: Member['onlineStatus']): string {
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

  onlineTextClass(status: Member['onlineStatus']): string {
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
