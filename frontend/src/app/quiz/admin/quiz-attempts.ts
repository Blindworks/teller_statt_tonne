import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { QuizService } from '../quiz.service';
import { COLOR_EMOJI, QuizApplicantStatus, QuizAttempt } from '../quiz.model';

type View = 'attempts' | 'applicants';

interface AttemptGroup {
  email: string;
  name: string;
  status: QuizApplicantStatus | null;
  attempts: QuizAttempt[];
}

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
  readonly applicants = signal<QuizApplicantStatus[]>([]);
  readonly selected = signal<QuizAttempt | null>(null);
  readonly errorMessage = signal<string | null>(null);
  readonly view = signal<View>('attempts');
  readonly unlockingEmail = signal<string | null>(null);

  readonly colorEmoji = COLOR_EMOJI;

  readonly attemptGroups = computed<AttemptGroup[]>(() => {
    const statusByEmail = new Map<string, QuizApplicantStatus>();
    for (const s of this.applicants()) {
      statusByEmail.set(s.email.toLowerCase(), s);
    }
    const groups = new Map<string, AttemptGroup>();
    for (const a of this.attempts()) {
      const key = (a.applicantEmail ?? '').toLowerCase();
      let group = groups.get(key);
      if (!group) {
        group = {
          email: a.applicantEmail,
          name: a.applicantName,
          status: statusByEmail.get(key) ?? null,
          attempts: [],
        };
        groups.set(key, group);
      }
      group.attempts.push(a);
    }
    return Array.from(groups.values()).sort((g1, g2) => {
      const t1 = g1.attempts[0]?.completedAt ?? '';
      const t2 = g2.attempts[0]?.completedAt ?? '';
      return t2.localeCompare(t1);
    });
  });

  constructor() {
    this.loadAttempts();
    this.loadApplicants();
  }

  switchView(v: View): void {
    this.view.set(v);
  }

  private loadAttempts(): void {
    this.service.listAttempts().subscribe({
      next: (xs) => this.attempts.set(xs),
      error: () => this.errorMessage.set('Versuche konnten nicht geladen werden.'),
    });
  }

  private loadApplicants(): void {
    this.service.listApplicants().subscribe({
      next: (xs) => this.applicants.set(xs),
      error: () => this.errorMessage.set('Bewerber-Status konnte nicht geladen werden.'),
    });
  }

  unlock(applicant: QuizApplicantStatus): void {
    if (!confirm(`Bewerber ${applicant.email} wirklich entsperren?`)) {
      return;
    }
    this.unlockingEmail.set(applicant.email);
    this.service.unlockApplicant(applicant.email).subscribe({
      next: () => {
        this.unlockingEmail.set(null);
        this.loadApplicants();
      },
      error: () => {
        this.unlockingEmail.set(null);
        this.errorMessage.set('Entsperren fehlgeschlagen.');
      },
    });
  }

  applicantStatusLabel(a: QuizApplicantStatus): string {
    if (a.passed) return 'Bestanden';
    if (a.locked) return 'Gesperrt';
    return 'Offen';
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
