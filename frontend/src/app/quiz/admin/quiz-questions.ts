import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ConfirmDialogService } from '../../shared/confirm-dialog/confirm-dialog.service';
import { QuizService } from '../quiz.service';
import { QuizQuestion } from '../quiz.model';

@Component({
  selector: 'app-quiz-questions',
  imports: [RouterLink],
  templateUrl: './quiz-questions.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class QuizQuestionsComponent {
  private readonly service = inject(QuizService);
  private readonly confirmDialog = inject(ConfirmDialogService);

  readonly questions = signal<QuizQuestion[]>([]);
  readonly errorMessage = signal<string | null>(null);

  constructor() {
    this.load();
  }

  load(): void {
    this.service.listQuestions().subscribe({
      next: (qs) => this.questions.set(qs),
      error: () => this.errorMessage.set('Fragen konnten nicht geladen werden.'),
    });
  }

  async delete(id: number): Promise<void> {
    const ok = await this.confirmDialog.ask({
      title: 'Frage löschen',
      message: 'Frage wirklich löschen?',
      confirmLabel: 'Löschen',
      tone: 'danger',
    });
    if (!ok) return;
    this.service.deleteQuestion(id).subscribe({
      next: () => this.load(),
      error: () => this.errorMessage.set('Frage konnte nicht gelöscht werden.'),
    });
  }

  correctCount(q: QuizQuestion): number {
    return q.answers.filter((a) => a.isCorrect).length;
  }

  knockoutCount(q: QuizQuestion): number {
    return q.answers.filter((a) => a.isKnockout).length;
  }
}
