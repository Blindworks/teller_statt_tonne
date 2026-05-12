import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FoodCategoryService } from '../../food-categories/food-category.service';
import { FoodCategory, emptyFoodCategory } from '../../food-categories/food-category.model';
import { ConfirmDialogService } from '../../shared/confirm-dialog/confirm-dialog.service';

@Component({
  selector: 'app-food-categories-admin',
  imports: [FormsModule, RouterLink],
  templateUrl: './food-categories-admin.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FoodCategoriesAdminComponent {
  private readonly service = inject(FoodCategoryService);
  private readonly confirmDialog = inject(ConfirmDialogService);

  readonly items = signal<FoodCategory[]>([]);
  readonly draft = signal<FoodCategory>(emptyFoodCategory());
  readonly editing = signal<FoodCategory | null>(null);
  readonly loadError = signal<string | null>(null);
  readonly saveError = signal<string | null>(null);
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

  resetDraft(): void {
    this.draft.set(emptyFoodCategory());
    this.editing.set(null);
    this.saveError.set(null);
  }

  startEdit(item: FoodCategory): void {
    this.editing.set(item);
    this.draft.set({ ...item });
    this.saveError.set(null);
  }

  save(): void {
    const dto = this.draft();
    if (!dto.name.trim()) {
      this.saveError.set('Name ist erforderlich.');
      return;
    }
    this.busy.set(true);
    const obs = this.editing()
      ? this.service.update(this.editing()!.id!, dto)
      : this.service.create(dto);
    obs.subscribe({
      next: (saved) => {
        this.busy.set(false);
        this.items.update((list) => {
          if (this.editing()) {
            return list.map((i) => (i.id === saved.id ? saved : i));
          }
          return [...list, saved];
        });
        this.resetDraft();
      },
      error: (err) => {
        this.busy.set(false);
        this.saveError.set(
          typeof err?.error === 'string' && err.error.length > 0
            ? err.error
            : 'Speichern fehlgeschlagen.',
        );
      },
    });
  }

  async remove(item: FoodCategory): Promise<void> {
    if (item.id == null) return;
    const ok = await this.confirmDialog.ask({
      title: 'Kategorie löschen?',
      message: `"${item.name}" wirklich löschen?`,
      confirmLabel: 'Löschen',
      tone: 'danger',
    });
    if (!ok) return;
    this.busy.set(true);
    this.service.delete(item.id).subscribe({
      next: () => {
        this.busy.set(false);
        this.items.update((list) => list.filter((i) => i.id !== item.id));
      },
      error: () => {
        this.busy.set(false);
        this.saveError.set('Löschen fehlgeschlagen.');
      },
    });
  }

  toggleActive(item: FoodCategory): void {
    if (item.id == null) return;
    this.service.update(item.id, { ...item, active: !item.active }).subscribe({
      next: (saved) => {
        this.items.update((list) => list.map((i) => (i.id === saved.id ? saved : i)));
      },
    });
  }

  updateDraft<K extends keyof FoodCategory>(key: K, value: FoodCategory[K]): void {
    this.draft.update((d) => ({ ...d, [key]: value }));
  }
}
