import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DistributionPointService } from '../distribution-point.service';
import { DistributionPoint } from '../distribution-point.model';

@Component({
  selector: 'app-distribution-points-list',
  imports: [RouterLink],
  templateUrl: './distribution-points-list.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DistributionPointsListComponent {
  private readonly service = inject(DistributionPointService);

  readonly items = signal<DistributionPoint[]>([]);
  readonly loadError = signal<string | null>(null);
  readonly actionError = signal<string | null>(null);
  readonly confirmDeleteId = signal<number | null>(null);
  readonly busy = signal(false);

  constructor() {
    this.reload();
  }

  reload(): void {
    this.service
      .list()
      .pipe(takeUntilDestroyed())
      .subscribe({
        next: (items) => {
          this.items.set(items);
          this.loadError.set(null);
        },
        error: () => this.loadError.set('Verteilerplätze konnten nicht geladen werden.'),
      });
  }

  locationOf(item: DistributionPoint): string {
    const parts = [item.postalCode, item.city].filter((p) => !!p && p.trim().length > 0);
    return parts.length ? parts.join(' ') : '–';
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
            : 'Verteilerplatz konnte nicht gelöscht werden.',
        );
        this.confirmDeleteId.set(null);
      },
    });
  }
}
