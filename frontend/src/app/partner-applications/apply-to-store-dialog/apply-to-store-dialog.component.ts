import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApplyToStoreDialogService } from './apply-to-store-dialog.service';
import { PartnerApplicationsService } from '../partner-applications.service';

@Component({
  selector: 'app-apply-to-store-dialog',
  imports: [FormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (target(); as t) {
      <div
        class="fixed inset-0 z-50 flex items-end md:items-center justify-center bg-black/60 px-4 py-6"
        (click)="cancel()"
        (keydown.escape)="cancel()"
        role="presentation"
      >
        <div
          class="w-full max-w-lg bg-surface-container-low rounded-2xl shadow-2xl p-6 md:p-8"
          (click)="$event.stopPropagation()"
          role="dialog"
          aria-modal="true"
          [attr.aria-label]="'Bewerbung bei ' + t.partnerName"
        >
          <header class="flex items-start justify-between gap-4 mb-4">
            <div>
              <h2 class="text-2xl font-extrabold text-on-surface">Bewerbung einreichen</h2>
              <p class="text-sm text-on-surface-variant mt-1">
                Du bewirbst dich bei <span class="font-bold">{{ t.partnerName }}</span
                >.
              </p>
            </div>
            <button
              type="button"
              (click)="cancel()"
              class="w-9 h-9 rounded-full hover:bg-surface-container-high text-on-surface-variant flex items-center justify-center"
              aria-label="Schließen"
            >
              <span class="material-symbols-outlined">close</span>
            </button>
          </header>

          <label class="block text-sm font-bold text-on-surface mb-2" for="apply-message">
            Optionale Nachricht (max. 1000 Zeichen)
          </label>
          <textarea
            id="apply-message"
            rows="4"
            maxlength="1000"
            [ngModel]="message()"
            (ngModelChange)="message.set($event)"
            placeholder="Schreibe der Teamleitung warum du dich bewirbst..."
            class="w-full bg-surface-container-lowest text-on-surface rounded-lg border border-outline-variant px-3 py-2 text-sm focus:outline-none focus-visible:ring-2 focus-visible:ring-primary"
          ></textarea>

          @if (errorMessage(); as msg) {
            <p
              class="mt-3 text-sm text-on-error-container bg-error-container/40 px-3 py-2 rounded-md"
            >
              {{ msg }}
            </p>
          }

          <div class="flex justify-end gap-3 mt-6">
            <button
              type="button"
              (click)="cancel()"
              [disabled]="submitting()"
              class="px-4 py-2 rounded-full text-sm font-bold text-on-surface-variant hover:bg-surface-container-high transition-colors disabled:opacity-50"
            >
              Abbrechen
            </button>
            <button
              type="button"
              (click)="submit()"
              [disabled]="submitting()"
              class="bg-primary text-on-primary px-5 py-2 rounded-full text-sm font-bold flex items-center gap-2 hover:bg-primary-dim transition-colors shadow-md shadow-primary/20 disabled:opacity-50"
            >
              <span class="material-symbols-outlined text-base">send</span>
              {{ submitting() ? 'Sende...' : 'Bewerbung absenden' }}
            </button>
          </div>
        </div>
      </div>
    }
  `,
})
export class ApplyToStoreDialogComponent {
  private readonly dialog = inject(ApplyToStoreDialogService);
  private readonly applications = inject(PartnerApplicationsService);

  readonly target = computed(() => this.dialog.target());
  readonly message = signal('');
  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  cancel(): void {
    if (this.submitting()) return;
    this.message.set('');
    this.errorMessage.set(null);
    this.dialog.close();
  }

  submit(): void {
    const t = this.target();
    if (!t) return;
    const trimmed = this.message().trim();
    this.submitting.set(true);
    this.errorMessage.set(null);
    this.applications.apply(t.partnerId, trimmed.length > 0 ? trimmed : null).subscribe({
      next: () => {
        this.submitting.set(false);
        this.message.set('');
        this.dialog.close();
      },
      error: (err) => {
        this.submitting.set(false);
        const e = err as { error?: unknown; status?: number };
        if (e?.status === 409) {
          this.errorMessage.set('Es liegt bereits eine offene Bewerbung vor.');
        } else if (typeof e?.error === 'string' && e.error.trim()) {
          this.errorMessage.set(e.error);
        } else {
          this.errorMessage.set('Bewerbung konnte nicht gesendet werden.');
        }
      },
    });
  }
}
