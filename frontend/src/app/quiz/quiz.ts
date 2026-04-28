import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { COLOR_EMOJI, QuizQuestion, QuizResult, SubmittedAnswer } from './quiz.model';
import { QuizService } from './quiz.service';

type Step = 'intro' | 'questions' | 'result' | 'error';

@Component({
  selector: 'app-quiz',
  imports: [FormsModule],
  templateUrl: './quiz.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class QuizComponent {
  private readonly service = inject(QuizService);

  readonly step = signal<Step>('intro');
  readonly applicantName = signal('');
  readonly applicantEmail = signal('');
  readonly questions = signal<QuizQuestion[]>([]);
  readonly selections = signal<Record<string, Set<string>>>({});
  readonly result = signal<QuizResult | null>(null);
  readonly errorMessage = signal<string | null>(null);
  readonly loading = signal(false);

  readonly colorEmoji = COLOR_EMOJI;

  readonly canSubmit = computed(() => {
    const name = this.applicantName().trim();
    const email = this.applicantEmail().trim();
    return name.length > 0 && email.length > 0;
  });

  start(): void {
    if (!this.canSubmit()) {
      this.errorMessage.set('Bitte Name und E-Mail angeben.');
      return;
    }
    this.errorMessage.set(null);
    this.loading.set(true);
    this.service.getPublicQuestions().subscribe({
      next: (qs) => {
        this.questions.set(qs);
        this.selections.set(
          qs.reduce(
            (acc, q) => {
              if (q.id) acc[q.id] = new Set<string>();
              return acc;
            },
            {} as Record<string, Set<string>>,
          ),
        );
        this.loading.set(false);
        if (qs.length === 0) {
          this.errorMessage.set('Es sind aktuell keine Fragen hinterlegt.');
          this.step.set('error');
        } else {
          this.step.set('questions');
        }
      },
      error: () => {
        this.loading.set(false);
        this.errorMessage.set('Fragen konnten nicht geladen werden.');
        this.step.set('error');
      },
    });
  }

  isSelected(questionId: string, answerId: string): boolean {
    return this.selections()[questionId]?.has(answerId) ?? false;
  }

  toggle(questionId: string, answerId: string): void {
    const current = this.selections();
    const set = new Set(current[questionId] ?? []);
    if (set.has(answerId)) {
      set.delete(answerId);
    } else {
      set.add(answerId);
    }
    this.selections.set({ ...current, [questionId]: set });
  }

  submit(): void {
    const answers: SubmittedAnswer[] = this.questions()
      .filter((q) => q.id)
      .map((q) => ({
        questionId: q.id as string,
        selectedAnswerIds: Array.from(this.selections()[q.id as string] ?? []),
      }));

    this.loading.set(true);
    this.errorMessage.set(null);
    this.service
      .submit({
        applicantName: this.applicantName().trim(),
        applicantEmail: this.applicantEmail().trim(),
        answers,
      })
      .subscribe({
        next: (res) => {
          this.result.set(res);
          this.loading.set(false);
          this.step.set('result');
        },
        error: (err) => {
          this.loading.set(false);
          this.errorMessage.set(
            typeof err?.error === 'string' ? err.error : 'Auswertung fehlgeschlagen.',
          );
        },
      });
  }

  restart(): void {
    this.applicantName.set('');
    this.applicantEmail.set('');
    this.questions.set([]);
    this.selections.set({});
    this.result.set(null);
    this.errorMessage.set(null);
    this.step.set('intro');
  }
}
