import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EventService } from '../event.service';
import { CharityEvent, EventScope } from '../event.model';
import { AuthService } from '../../auth/auth.service';

@Component({
  selector: 'app-events-list',
  imports: [RouterLink],
  templateUrl: './events-list.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EventsListComponent {
  private readonly service = inject(EventService);
  private readonly auth = inject(AuthService);

  readonly items = signal<CharityEvent[]>([]);
  readonly loadError = signal<string | null>(null);
  readonly actionError = signal<string | null>(null);
  readonly confirmDeleteId = signal<number | null>(null);
  readonly busy = signal(false);
  readonly scope = signal<EventScope>('active');

  readonly canManage = computed(() => {
    const roles = this.auth.currentUser()?.roles ?? [];
    return roles.includes('ADMINISTRATOR') || roles.includes('TEAMLEITER');
  });

  constructor() {
    this.reload();
  }

  reload(): void {
    this.service
      .list(this.scope())
      .pipe(takeUntilDestroyed())
      .subscribe({
        next: (items) => {
          this.items.set(items);
          this.loadError.set(null);
        },
        error: () => this.loadError.set('Veranstaltungen konnten nicht geladen werden.'),
      });
  }

  setScope(scope: EventScope): void {
    if (this.scope() === scope) return;
    this.scope.set(scope);
    this.reload();
  }

  locationOf(item: CharityEvent): string {
    const parts = [item.postalCode, item.city].filter((p): p is string => !!p && p.trim().length > 0);
    return parts.length ? parts.join(' ') : '–';
  }

  periodOf(item: CharityEvent): string {
    if (item.startDate === item.endDate) return item.startDate;
    return `${item.startDate} – ${item.endDate}`;
  }

  askDelete(id: number | null): void {
    if (id == null) return;
    this.actionError.set(null);
    this.confirmDeleteId.set(id);
  }

  cancelDelete(): void {
    this.confirmDeleteId.set(null);
  }

  confirmDelete(): void {
    const id = this.confirmDeleteId();
    if (id == null) return;
    this.busy.set(true);
    this.service.remove(id).subscribe({
      next: () => {
        this.busy.set(false);
        this.confirmDeleteId.set(null);
        this.items.update((list) => list.filter((i) => i.id !== id));
      },
      error: (err) => {
        this.busy.set(false);
        this.actionError.set(
          typeof err?.error === 'string' && err.error.length > 0
            ? err.error
            : 'Veranstaltung konnte nicht gelöscht werden.',
        );
        this.confirmDeleteId.set(null);
      },
    });
  }
}
