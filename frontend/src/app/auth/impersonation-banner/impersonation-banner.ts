import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../auth.service';

@Component({
  selector: 'app-impersonation-banner',
  standalone: true,
  template: `
    @if (auth.isImpersonating() && auth.currentUser(); as user) {
      <div
        class="sticky top-0 z-50 flex items-center justify-between gap-3 bg-amber-500 px-4 py-2 text-sm font-medium text-white shadow"
      >
        <span>
          Du bist als Test-Retter <strong>{{ user.email }}</strong> angemeldet.
        </span>
        <button
          type="button"
          class="rounded-md bg-white/20 px-3 py-1 text-xs font-semibold uppercase tracking-wide hover:bg-white/30"
          (click)="endImpersonation()"
        >
          Zurück zum Admin
        </button>
      </div>
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ImpersonationBannerComponent {
  protected readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  endImpersonation(): void {
    if (this.auth.endImpersonation()) {
      this.router.navigateByUrl('/admin/onboarding');
    }
  }
}
