import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { QuizService } from '../quiz.service';
import { COLOR_LABELS, QuizColor, QuizResultCategory, emptyCategory } from '../quiz.model';

@Component({
  selector: 'app-quiz-categories',
  imports: [RouterLink, FormsModule],
  templateUrl: './quiz-categories.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class QuizCategoriesComponent {
  private readonly service = inject(QuizService);

  readonly categories = signal<QuizResultCategory[]>([]);
  readonly errorMessage = signal<string | null>(null);
  readonly draft = signal<QuizResultCategory>(emptyCategory());

  readonly colors: QuizColor[] = ['GREEN', 'YELLOW', 'RED'];
  readonly colorLabels = COLOR_LABELS;

  constructor() {
    this.load();
  }

  load(): void {
    this.service.listCategories().subscribe({
      next: (cs) => this.categories.set(cs),
      error: () => this.errorMessage.set('Kategorien konnten nicht geladen werden.'),
    });
  }

  updateDraft<K extends keyof QuizResultCategory>(key: K, value: QuizResultCategory[K]): void {
    this.draft.set({ ...this.draft(), [key]: value });
  }

  add(): void {
    this.errorMessage.set(null);
    this.service.createCategory(this.draft()).subscribe({
      next: () => {
        this.draft.set(emptyCategory());
        this.load();
      },
      error: (err) =>
        this.errorMessage.set(
          typeof err?.error === 'string' ? err.error : 'Speichern fehlgeschlagen.',
        ),
    });
  }

  saveExisting(category: QuizResultCategory): void {
    if (!category.id) return;
    this.service.updateCategory(category.id, category).subscribe({
      next: () => this.load(),
      error: () => this.errorMessage.set('Aktualisierung fehlgeschlagen.'),
    });
  }

  delete(id: string): void {
    if (!confirm('Kategorie löschen?')) return;
    this.service.deleteCategory(id).subscribe({
      next: () => this.load(),
      error: () => this.errorMessage.set('Löschen fehlgeschlagen.'),
    });
  }

  trackById(_: number, c: QuizResultCategory): string {
    return c.id ?? '';
  }
}
