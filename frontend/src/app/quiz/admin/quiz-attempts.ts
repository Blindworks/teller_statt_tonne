import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
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
  private readonly router = inject(Router);

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

  createUser(attempt: QuizAttempt): void {
    const parts = attempt.applicantName.trim().split(/\s+/).filter(Boolean);
    const lastName = parts.length > 1 ? parts[parts.length - 1]! : '';
    const firstName = parts.length > 1 ? parts.slice(0, -1).join(' ') : (parts[0] ?? '');
    this.router.navigate(['/users/new'], {
      queryParams: {
        firstName,
        lastName,
        email: attempt.applicantEmail,
      },
    });
  }

  initials(name: string): string {
    const parts = name.trim().split(/\s+/).filter(Boolean);
    if (parts.length === 0) return '?';
    const first = parts[0]!.charAt(0);
    const last = parts.length > 1 ? parts[parts.length - 1]!.charAt(0) : '';
    return (first + last).toUpperCase();
  }
}
