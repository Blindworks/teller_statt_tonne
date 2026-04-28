import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { QuizService } from '../quiz.service';
import { COLOR_EMOJI, QuizAttempt } from '../quiz.model';

@Component({
  selector: 'app-quiz-attempts',
  imports: [RouterLink, DatePipe],
  templateUrl: './quiz-attempts.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class QuizAttemptsComponent {
  private readonly service = inject(QuizService);

  readonly attempts = signal<QuizAttempt[]>([]);
  readonly selected = signal<QuizAttempt | null>(null);
  readonly errorMessage = signal<string | null>(null);

  readonly colorEmoji = COLOR_EMOJI;

  constructor() {
    this.service.listAttempts().subscribe({
      next: (xs) => this.attempts.set(xs),
      error: () => this.errorMessage.set('Versuche konnten nicht geladen werden.'),
    });
  }

  open(attempt: QuizAttempt): void {
    this.selected.set(attempt);
  }

  close(): void {
    this.selected.set(null);
  }
}
