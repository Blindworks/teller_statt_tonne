import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { ReactiveFormsModule, NonNullableFormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../auth.service';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './login.html',
  styleUrl: './login.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LoginComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(NonNullableFormBuilder);

  readonly form = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]],
  });

  readonly submitting = signal(false);
  readonly error = signal<string | null>(null);

  private static readonly LOCKED_MESSAGE =
    'Dein Konto ist gesperrt. Bitte wende dich an einen Administrator.';

  constructor() {
    try {
      if (sessionStorage.getItem('tst.lockReason') === 'locked') {
        this.error.set(LoginComponent.LOCKED_MESSAGE);
        sessionStorage.removeItem('tst.lockReason');
      }
    } catch {
      /* sessionStorage unavailable — ignore */
    }
  }

  submit(): void {
    if (this.form.invalid || this.submitting()) return;
    this.submitting.set(true);
    this.error.set(null);
    const { email, password } = this.form.getRawValue();
    this.auth.login(email, password).subscribe({
      next: () => {
        this.submitting.set(false);
        const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl') ?? '/dashboard';
        this.router.navigateByUrl(returnUrl);
      },
      error: (err) => {
        this.submitting.set(false);
        if (err?.error === 'Account locked') {
          this.error.set(LoginComponent.LOCKED_MESSAGE);
        } else {
          this.error.set('E-Mail oder Passwort ist falsch.');
        }
      },
    });
  }
}
