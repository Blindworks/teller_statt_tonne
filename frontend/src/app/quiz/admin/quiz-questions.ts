import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
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

  delete(id: string): void {
    if (!confirm('Frage wirklich löschen?')) return;
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
