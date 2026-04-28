import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import {
  FormArray,
  FormBuilder,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { QuizService } from '../quiz.service';
import { ALLOWED_WEIGHTS, QuizAnswer, QuizQuestion, emptyQuestion } from '../quiz.model';

type AnswerForm = FormGroup<{
  id: FormControl<string | null>;
  text: FormControl<string>;
  isCorrect: FormControl<boolean>;
  isKnockout: FormControl<boolean>;
}>;

type QuestionForm = FormGroup<{
  text: FormControl<string>;
  weight: FormControl<number>;
  answers: FormArray<AnswerForm>;
}>;

@Component({
  selector: 'app-question-edit',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './question-edit.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class QuestionEditComponent {
  private readonly fb = inject(FormBuilder);
  private readonly service = inject(QuizService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly weights = ALLOWED_WEIGHTS;
  readonly questionId = signal<string | null>(null);
  readonly saving = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form: QuestionForm = this.buildForm();

  constructor() {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.questionId.set(id);
      this.service.getQuestion(id).subscribe({
        next: (q) => this.patchForm(q),
        error: () => this.errorMessage.set('Frage konnte nicht geladen werden.'),
      });
    }
  }

  get answers(): FormArray<AnswerForm> {
    return this.form.controls.answers;
  }

  get isEdit(): boolean {
    return this.questionId() !== null;
  }

  addAnswer(): void {
    this.answers.push(this.answerGroup({ id: null, text: '', isCorrect: false, isKnockout: false }));
  }

  removeAnswer(index: number): void {
    if (this.answers.length <= 1) return;
    this.answers.removeAt(index);
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const payload = this.toQuestion();
    this.saving.set(true);
    this.errorMessage.set(null);
    const req$ = this.isEdit
      ? this.service.updateQuestion(this.questionId()!, payload)
      : this.service.createQuestion(payload);
    req$.subscribe({
      next: () => {
        this.saving.set(false);
        this.router.navigate(['/admin/quiz/questions']);
      },
      error: (err) => {
        this.saving.set(false);
        this.errorMessage.set(
          typeof err?.error === 'string' ? err.error : 'Speichern fehlgeschlagen.',
        );
      },
    });
  }

  private buildForm(): QuestionForm {
    const defaults = emptyQuestion();
    return this.fb.group({
      text: this.fb.nonNullable.control(defaults.text, Validators.required),
      weight: this.fb.nonNullable.control(defaults.weight, Validators.required),
      answers: this.fb.array(defaults.answers.map((a) => this.answerGroup(a))),
    });
  }

  private answerGroup(a: QuizAnswer): AnswerForm {
    return this.fb.group({
      id: this.fb.control<string | null>(a.id),
      text: this.fb.nonNullable.control(a.text, Validators.required),
      isCorrect: this.fb.nonNullable.control(Boolean(a.isCorrect)),
      isKnockout: this.fb.nonNullable.control(Boolean(a.isKnockout)),
    });
  }

  private patchForm(q: QuizQuestion): void {
    this.form.patchValue({ text: q.text, weight: Number(q.weight) });
    this.answers.clear();
    for (const a of q.answers) {
      this.answers.push(this.answerGroup(a));
    }
  }

  private toQuestion(): QuizQuestion {
    const raw = this.form.getRawValue();
    return {
      id: this.questionId(),
      text: raw.text,
      weight: raw.weight,
      answers: raw.answers.map((a) => ({
        id: a.id,
        text: a.text,
        isCorrect: a.isCorrect,
        isKnockout: a.isKnockout,
      })),
    };
  }
}
