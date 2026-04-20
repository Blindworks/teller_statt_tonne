import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Subject, debounceTime, distinctUntilChanged, switchMap } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MemberFilter, MemberService } from '../member.service';
import { Member, MemberType } from '../member.model';

type FilterChip = { label: string; type: MemberType | null; activeOnly: boolean };

@Component({
  selector: 'app-members-list',
  imports: [RouterLink, FormsModule],
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

  readonly filters: FilterChip[] = [
    { label: 'Alle Mitglieder', type: null, activeOnly: false },
    { label: 'Botschafter', type: 'BOTSCHAFTER', activeOnly: false },
    { label: 'Foodsaver', type: 'FOODSAVER', activeOnly: false },
    { label: 'Jetzt aktiv', type: null, activeOnly: true },
  ];

  readonly totalCount = computed(() => this.members().length);

  constructor() {
    this.reload$
      .pipe(
        debounceTime(250),
        distinctUntilChanged(
          (a, b) => a.type === b.type && a.activeOnly === b.activeOnly && a.q === b.q,
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

  typeBadgeClass(type: MemberType): string {
    switch (type) {
      case 'BOTSCHAFTER':
        return 'bg-tertiary-container text-on-tertiary-fixed';
      case 'FOODSAVER':
        return 'bg-primary-container text-on-primary-container';
      case 'NEW_MEMBER':
        return 'bg-secondary-container text-on-secondary-container';
    }
  }

  typeLabel(type: MemberType): string {
    switch (type) {
      case 'BOTSCHAFTER':
        return 'Botschafter';
      case 'FOODSAVER':
        return 'Foodsaver';
      case 'NEW_MEMBER':
        return 'Neu';
    }
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
    const chip = this.filters[this.activeFilterIndex()];
    this.reload$.next({ type: chip.type, activeOnly: chip.activeOnly, q: this.search() });
  }
}
