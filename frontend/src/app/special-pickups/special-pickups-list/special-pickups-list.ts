import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { SpecialPickupService } from '../special-pickup.service';
import { SpecialPickup, SpecialPickupScope } from '../special-pickup.model';
import { AuthService } from '../../auth/auth.service';

@Component({
  selector: 'app-special-pickups-list',
  imports: [RouterLink],
  templateUrl: './special-pickups-list.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SpecialPickupsListComponent {
  private readonly service = inject(SpecialPickupService);
  private readonly auth = inject(AuthService);

  readonly items = signal<SpecialPickup[]>([]);
  readonly loadError = signal<string | null>(null);
  readonly actionError = signal<string | null>(null);
  readonly confirmDeleteId = signal<number | null>(null);
  readonly busy = signal(false);
  readonly scope = signal<SpecialPickupScope>('active');

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
        error: () => this.loadError.set('Sonderabholungen konnten nicht geladen werden.'),
      });
  }

  setScope(scope: SpecialPickupScope): void {
    if (this.scope() === scope) return;
    this.scope.set(scope);
    this.reload();
  }

  locationOf(item: SpecialPickup): string {
    const parts = [item.postalCode, item.city].filter((p): p is string => !!p && p.trim().length > 0);
    return parts.length ? parts.join(' ') : '–';
  }

  periodOf(item: SpecialPickup): string {
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
            : 'Sonderabholung konnte nicht gelöscht werden.',
        );
        this.confirmDeleteId.set(null);
      },
    });
  }
}
