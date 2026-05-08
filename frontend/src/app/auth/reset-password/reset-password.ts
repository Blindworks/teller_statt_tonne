import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import {
  AbstractControl,
  ReactiveFormsModule,
  NonNullableFormBuilder,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../auth.service';

function passwordsMatch(group: AbstractControl): ValidationErrors | null {
  const pw = group.get('newPassword')?.value;
  const cf = group.get('confirmPassword')?.value;
  return pw && cf && pw !== cf ? { passwordsMismatch: true } : null;
}

@Component({
  selector: 'app-reset-password',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './reset-password.html',
  styleUrl: './reset-password.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ResetPasswordComponent {
  private readonly auth = inject(AuthService);
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly token = this.route.snapshot.paramMap.get('token') ?? '';

  readonly form = this.fb.group(
    {
      newPassword: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(100)]],
      confirmPassword: ['', [Validators.required]],
    },
    { validators: passwordsMatch },
  );

  readonly submitting = signal(false);
  readonly error = signal<string | null>(null);
  readonly success = signal(false);

  submit(): void {
    if (this.form.invalid || this.submitting() || !this.token) return;
    this.submitting.set(true);
    this.error.set(null);
    const { newPassword } = this.form.getRawValue();
    this.auth.resetPassword(this.token, newPassword).subscribe({
      next: () => {
        this.submitting.set(false);
        this.success.set(true);
        setTimeout(() => this.router.navigateByUrl('/login'), 1500);
      },
      error: (err) => {
        this.submitting.set(false);
        if (err?.status === 400) {
          this.error.set('Der Link ist ungültig oder abgelaufen. Bitte fordere einen neuen an.');
        } else {
          this.error.set('Passwort konnte nicht zurückgesetzt werden. Bitte versuche es erneut.');
        }
      },
    });
  }
}
