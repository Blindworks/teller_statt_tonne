import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { PartnerCategoryService } from '../partner-category.service';
import { PartnerCategory } from '../partner-category.model';
import { PartnerCategoryRegistry } from '../../../partners/partner-category-registry.service';

@Component({
  selector: 'app-partner-categories-list',
  imports: [RouterLink],
  templateUrl: './partner-categories-list.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PartnerCategoriesListComponent {
  private readonly service = inject(PartnerCategoryService);
  private readonly registry = inject(PartnerCategoryRegistry);

  readonly items = signal<PartnerCategory[]>([]);
  readonly loadError = signal<string | null>(null);
  readonly actionError = signal<string | null>(null);
  readonly confirmDeleteId = signal<number | null>(null);
  readonly busy = signal(false);

  constructor() {
    this.reload();
  }

  reload(): void {
    this.service
      .listAll()
      .pipe(takeUntilDestroyed())
      .subscribe({
        next: (items) => {
          this.items.set(items);
          this.loadError.set(null);
        },
        error: () => this.loadError.set('Kategorien konnten nicht geladen werden.'),
      });
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
        this.registry.reload();
      },
      error: (err) => {
        this.busy.set(false);
        this.actionError.set(
          typeof err?.error === 'string' && err.error.length > 0
            ? err.error
            : 'Kategorie konnte nicht gelöscht werden.',
        );
        this.confirmDeleteId.set(null);
      },
    });
  }

  toggleActive(item: PartnerCategory): void {
    if (item.id == null) return;
    const updated: PartnerCategory = { ...item, active: !item.active };
    this.service.update(item.id, updated).subscribe({
      next: (saved) => {
        this.items.update((list) => list.map((i) => (i.id === saved.id ? saved : i)));
        this.registry.reload();
      },
      error: (err) => {
        this.actionError.set(
          typeof err?.error === 'string' && err.error.length > 0
            ? err.error
            : 'Aktualisierung fehlgeschlagen.',
        );
      },
    });
  }
}
