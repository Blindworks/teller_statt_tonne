import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AppointmentService } from '../appointment.service';
import { Appointment } from '../appointment.model';
import { formatRange } from '../format';
import { AuthService } from '../../auth/auth.service';

type Scope = 'upcoming' | 'past';

@Component({
  selector: 'app-appointments-list',
  imports: [RouterLink],
  templateUrl: './appointments-list.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AppointmentsListComponent {
  private readonly service = inject(AppointmentService);
  private readonly auth = inject(AuthService);

  readonly items = signal<Appointment[]>([]);
  readonly loadError = signal<string | null>(null);
  readonly actionError = signal<string | null>(null);
  readonly confirmDeleteId = signal<number | null>(null);
  readonly busy = signal(false);
  readonly scope = signal<Scope>('upcoming');

  readonly canCreate = computed(() => {
    const roles = this.auth.currentUser()?.roles ?? [];
    return roles.includes('ADMINISTRATOR') || roles.includes('TEAMLEITER');
  });

  constructor() {
    this.reload();
    this.service.refreshUnreadCount().pipe(takeUntilDestroyed()).subscribe();
  }

  reload(): void {
    this.service
      .list(this.scope() === 'upcoming')
      .pipe(takeUntilDestroyed())
      .subscribe({
        next: (items) => {
          this.items.set(items);
          this.loadError.set(null);
        },
        error: () => this.loadError.set('Termine konnten nicht geladen werden.'),
      });
  }

  setScope(scope: Scope): void {
    if (this.scope() === scope) return;
    this.scope.set(scope);
    this.reload();
  }

  rangeOf(item: Appointment): string {
    return formatRange(item.startTime, item.endTime);
  }

  rolesLabel(item: Appointment): string {
    if (item.isPublic && item.targetRoles.length === 0) return 'Öffentlich';
    const parts = item.targetRoles.map((r) => r.label || r.name);
    if (item.isPublic) parts.unshift('Öffentlich');
    return parts.join(', ') || '–';
  }

  askDelete(id: number): void {
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
        this.service.refreshUnreadCount().subscribe();
      },
      error: (err) => {
        this.busy.set(false);
        this.actionError.set(
          typeof err?.error === 'string' && err.error.length > 0
            ? err.error
            : 'Termin konnte nicht gelöscht werden.',
        );
        this.confirmDeleteId.set(null);
      },
    });
  }
}
