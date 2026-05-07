import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ConfirmDialogService } from '../../shared/confirm-dialog/confirm-dialog.service';
import { PartnerApplicationsService } from '../partner-applications.service';
import { APPLICATION_STATUS_LABELS, PartnerApplication } from '../partner-application.model';

@Component({
  selector: 'app-my-applications',
  imports: [DatePipe, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <main class="pt-24 pb-32 md:pb-12 px-6 md:pl-80 md:pr-12 max-w-screen-2xl mx-auto min-h-screen">
      <header class="mb-10">
        <h1 class="text-4xl font-extrabold text-on-surface">Meine Bewerbungen</h1>
        <p class="text-on-surface-variant mt-2">
          Status deiner Bewerbungen auf Betriebe.
        </p>
      </header>

      @if (loading()) {
        <p class="text-on-surface-variant">Lade Bewerbungen…</p>
      } @else if (errorMessage(); as msg) {
        <p class="px-4 py-3 rounded-lg bg-error-container text-on-error-container">{{ msg }}</p>
      } @else if (applications().length === 0) {
        <div class="bg-surface-container-low rounded-xl p-12 text-center text-on-surface-variant">
          <span class="material-symbols-outlined text-5xl text-primary mb-4">handshake</span>
          <p class="text-lg font-bold text-on-surface">Noch keine Bewerbungen</p>
          <p class="mt-1">
            Geh auf <a class="text-primary underline" routerLink="/stores">Betriebe</a>
            und bewirb dich auf einen Betrieb deiner Wahl.
          </p>
        </div>
      } @else {
        <ul class="space-y-4">
          @for (app of applications(); track app.id) {
            <li class="bg-surface-container-low rounded-xl p-6">
              <div class="flex items-start justify-between gap-4 flex-wrap">
                <div>
                  <h2 class="text-xl font-bold text-on-surface">{{ app.partnerName }}</h2>
                  @if (app.partnerStreet || app.partnerCity) {
                    <p class="text-xs text-on-surface-variant flex items-center gap-1 mt-0.5">
                      <span class="material-symbols-outlined text-xs">location_on</span>
                      {{ app.partnerStreet }}, {{ app.partnerPostalCode }}
                      {{ app.partnerCity }}
                    </p>
                  }
                  <p class="text-xs text-on-surface-variant mt-1">
                    eingereicht: {{ app.createdAt | date: 'dd.MM.yyyy HH:mm' }}
                    @if (app.decidedAt) {
                      · entschieden: {{ app.decidedAt | date: 'dd.MM.yyyy HH:mm' }}
                    }
                  </p>
                </div>
                <span
                  class="px-3 py-1 rounded-full text-[10px] font-bold uppercase tracking-wider"
                  [class]="badgeClass(app)"
                >
                  {{ statusLabels[app.status] }}
                </span>
              </div>
              @if (app.message) {
                <p class="mt-3 text-sm text-on-surface-variant whitespace-pre-line">
                  <span class="font-bold text-on-surface">Deine Nachricht:</span> {{ app.message }}
                </p>
              }
              @if (app.decisionReason) {
                <p class="mt-3 text-sm text-on-surface-variant whitespace-pre-line">
                  <span class="font-bold text-on-surface">Begründung:</span>
                  {{ app.decisionReason }}
                </p>
              }
              @if (app.status === 'PENDING') {
                <div class="mt-4 flex justify-end">
                  <button
                    type="button"
                    (click)="withdraw(app)"
                    [disabled]="busyId() === app.id"
                    class="px-4 py-2 rounded-full text-sm font-bold border border-outline-variant text-on-surface-variant hover:bg-surface-container-high transition-colors disabled:opacity-50"
                  >
                    Bewerbung zurückziehen
                  </button>
                </div>
              }
            </li>
          }
        </ul>
      }
    </main>
  `,
})
export class MyApplicationsComponent {
  private readonly service = inject(PartnerApplicationsService);
  private readonly confirmDialog = inject(ConfirmDialogService);

  readonly applications = signal<PartnerApplication[]>([]);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);
  readonly busyId = signal<number | null>(null);
  readonly statusLabels = APPLICATION_STATUS_LABELS;

  constructor() {
    this.load();
  }

  badgeClass(app: PartnerApplication): string {
    switch (app.status) {
      case 'PENDING':
        return 'bg-tertiary-container text-on-tertiary-container';
      case 'APPROVED':
        return 'bg-primary-container text-on-primary-container';
      case 'REJECTED':
        return 'bg-error-container text-on-error-container';
      case 'WITHDRAWN':
        return 'bg-surface-container-high text-on-surface-variant';
      default:
        return 'bg-surface-container text-on-surface';
    }
  }

  async withdraw(app: PartnerApplication): Promise<void> {
    const ok = await this.confirmDialog.ask({
      title: 'Bewerbung zurückziehen',
      message: `Bewerbung bei ${app.partnerName} wirklich zurückziehen?`,
      confirmLabel: 'Zurückziehen',
      tone: 'danger',
    });
    if (!ok) return;
    this.busyId.set(app.id);
    this.service.withdraw(app.id).subscribe({
      next: () => {
        this.busyId.set(null);
        this.load();
      },
      error: () => {
        this.busyId.set(null);
        this.errorMessage.set('Bewerbung konnte nicht zurückgezogen werden.');
      },
    });
  }

  private load(): void {
    this.loading.set(true);
    this.errorMessage.set(null);
    this.service.listMine().subscribe({
      next: (apps) => {
        this.applications.set(apps);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.errorMessage.set('Bewerbungen konnten nicht geladen werden.');
      },
    });
  }
}
