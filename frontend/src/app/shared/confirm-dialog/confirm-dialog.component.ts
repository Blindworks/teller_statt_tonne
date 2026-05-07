import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { ConfirmDialogService } from './confirm-dialog.service';

@Component({
  selector: 'app-confirm-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (request(); as r) {
      <div
        class="fixed inset-0 z-50 flex items-end md:items-center justify-center bg-black/60 px-4 py-6"
        (click)="cancel()"
        (keydown.escape)="cancel()"
        role="presentation"
      >
        <div
          class="w-full max-w-md bg-surface-container-low rounded-2xl shadow-2xl p-6 md:p-8"
          (click)="$event.stopPropagation()"
          role="dialog"
          aria-modal="true"
          [attr.aria-label]="r.title"
        >
          <h2 class="text-xl font-extrabold text-on-surface mb-2">{{ r.title }}</h2>
          <p class="text-sm text-on-surface-variant whitespace-pre-line">{{ r.message }}</p>
          <div class="flex justify-end gap-3 mt-6">
            <button
              type="button"
              (click)="cancel()"
              class="px-4 py-2 rounded-full text-sm font-bold text-on-surface-variant hover:bg-surface-container-high transition-colors"
            >
              {{ r.cancelLabel ?? 'Abbrechen' }}
            </button>
            <button
              type="button"
              (click)="confirm()"
              [class]="confirmButtonClass()"
            >
              {{ r.confirmLabel ?? 'Bestätigen' }}
            </button>
          </div>
        </div>
      </div>
    }
  `,
})
export class ConfirmDialogComponent {
  private readonly service = inject(ConfirmDialogService);

  readonly request = this.service.request;

  readonly confirmButtonClass = computed(() => {
    const tone = this.request()?.tone ?? 'primary';
    const base = 'px-5 py-2 rounded-full text-sm font-bold transition-colors shadow-md';
    return tone === 'danger'
      ? `${base} bg-error text-on-error hover:bg-error/90 shadow-error/20`
      : `${base} bg-primary text-on-primary hover:bg-primary-dim shadow-primary/20`;
  });

  cancel(): void {
    this.service.resolve(false);
  }

  confirm(): void {
    this.service.resolve(true);
  }
}
