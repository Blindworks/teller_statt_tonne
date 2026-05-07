import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { PartnerService } from '../../partners/partner.service';
import { Partner } from '../../partners/partner.model';
import { ConfirmDialogService } from '../../shared/confirm-dialog/confirm-dialog.service';
import { PartnerApplicationsService } from '../partner-applications.service';
import {
  APPLICATION_STATUS_LABELS,
  ApplicationStatus,
  PartnerApplication,
} from '../partner-application.model';

interface PartnerWithApplications {
  partner: Partner;
  applications: PartnerApplication[];
}

@Component({
  selector: 'app-admin-applications',
  imports: [DatePipe, FormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <main class="pt-24 pb-32 md:pb-12 px-6 md:pl-80 md:pr-12 max-w-screen-2xl mx-auto min-h-screen">
      <header class="mb-10 flex flex-col md:flex-row justify-between md:items-end gap-6">
        <div>
          <h1 class="text-4xl font-extrabold text-on-surface">Bewerbungen auf Betriebe</h1>
          <p class="text-on-surface-variant mt-2">
            Bewerbungen prüfen, annehmen oder ablehnen.
          </p>
        </div>
        <label class="flex items-center gap-2 text-sm">
          <span class="text-on-surface-variant">Status:</span>
          <select
            [ngModel]="statusFilter()"
            (ngModelChange)="setStatusFilter($event)"
            class="bg-surface-container-low text-on-surface rounded-full border border-outline-variant px-4 py-2 text-sm focus:outline-none focus-visible:ring-2 focus-visible:ring-primary"
          >
            <option value="PENDING">Offen</option>
            <option value="APPROVED">Angenommen</option>
            <option value="REJECTED">Abgelehnt</option>
            <option value="WITHDRAWN">Zurückgezogen</option>
            <option value="ALL">Alle</option>
          </select>
        </label>
      </header>

      @if (loading()) {
        <p class="text-on-surface-variant">Lade Bewerbungen…</p>
      } @else if (errorMessage(); as msg) {
        <p class="px-4 py-3 rounded-lg bg-error-container text-on-error-container">{{ msg }}</p>
      } @else if (filteredGroups().length === 0) {
        <div class="bg-surface-container-low rounded-xl p-12 text-center text-on-surface-variant">
          <span class="material-symbols-outlined text-5xl text-primary mb-4">how_to_reg</span>
          <p class="text-lg font-bold text-on-surface">Keine Bewerbungen für diesen Filter</p>
        </div>
      } @else {
        <div class="space-y-8">
          @for (group of filteredGroups(); track group.partner.id) {
            <section>
              <header class="mb-3">
                <h2 class="text-xl font-bold text-on-surface">
                  {{ group.partner.name }}
                  <span class="text-sm text-on-surface-variant font-normal">
                    ({{ group.applications.length }})
                  </span>
                </h2>
                <p class="text-xs text-on-surface-variant flex items-center gap-1 mt-0.5">
                  <span class="material-symbols-outlined text-xs">location_on</span>
                  {{ group.partner.street }}, {{ group.partner.postalCode }}
                  {{ group.partner.city }}
                </p>
              </header>
              <ul class="space-y-3">
                @for (app of group.applications; track app.id) {
                  <li class="bg-surface-container-low rounded-xl p-5">
                    <div class="flex items-start justify-between gap-4 flex-wrap">
                      <div>
                        <p class="font-bold text-on-surface">
                          {{ app.userFirstName }} {{ app.userLastName }}
                        </p>
                        <p class="text-xs text-on-surface-variant">{{ app.userEmail }}</p>
                        <p class="text-xs text-on-surface-variant mt-1">
                          eingereicht: {{ app.createdAt | date: 'dd.MM.yyyy HH:mm' }}
                          @if (app.decidedAt) {
                            · entschieden:
                            {{ app.decidedAt | date: 'dd.MM.yyyy HH:mm' }}
                            @if (app.decidedByDisplayName) {
                              von {{ app.decidedByDisplayName }}
                            }
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
                        <span class="font-bold text-on-surface">Nachricht:</span>
                        {{ app.message }}
                      </p>
                    }
                    @if (app.decisionReason) {
                      <p class="mt-2 text-sm text-on-surface-variant whitespace-pre-line">
                        <span class="font-bold text-on-surface">Begründung:</span>
                        {{ app.decisionReason }}
                      </p>
                    }
                    @if (app.status === 'PENDING') {
                      <div class="mt-4 flex flex-col gap-3">
                        @if (rejectingId() === app.id) {
                          <textarea
                            rows="2"
                            maxlength="1000"
                            [ngModel]="rejectReason()"
                            (ngModelChange)="rejectReason.set($event)"
                            placeholder="Optionale Begründung..."
                            class="w-full bg-surface-container-lowest text-on-surface rounded-lg border border-outline-variant px-3 py-2 text-sm"
                          ></textarea>
                        }
                        <div class="flex justify-end gap-2 flex-wrap">
                          @if (rejectingId() === app.id) {
                            <button
                              type="button"
                              (click)="cancelReject()"
                              class="px-4 py-2 rounded-full text-sm font-bold text-on-surface-variant hover:bg-surface-container-high"
                            >
                              Abbrechen
                            </button>
                            <button
                              type="button"
                              (click)="confirmReject(app)"
                              [disabled]="busyId() === app.id"
                              class="bg-error text-on-error px-4 py-2 rounded-full text-sm font-bold disabled:opacity-50"
                            >
                              Ablehnen bestätigen
                            </button>
                          } @else {
                            <button
                              type="button"
                              (click)="startReject(app)"
                              [disabled]="busyId() === app.id"
                              class="px-4 py-2 rounded-full text-sm font-bold border border-outline-variant text-on-surface-variant hover:bg-surface-container-high disabled:opacity-50"
                            >
                              Ablehnen
                            </button>
                            <button
                              type="button"
                              (click)="approve(app)"
                              [disabled]="busyId() === app.id"
                              class="bg-primary text-on-primary px-4 py-2 rounded-full text-sm font-bold flex items-center gap-2 hover:bg-primary-dim disabled:opacity-50"
                            >
                              <span class="material-symbols-outlined text-base">check</span>
                              Annehmen
                            </button>
                          }
                        </div>
                      </div>
                    }
                  </li>
                }
              </ul>
            </section>
          }
        </div>
      }
    </main>
  `,
})
export class AdminApplicationsComponent {
  private readonly partnerService = inject(PartnerService);
  private readonly applicationsService = inject(PartnerApplicationsService);
  private readonly confirmDialog = inject(ConfirmDialogService);

  readonly partners = signal<Partner[]>([]);
  readonly applications = signal<PartnerApplication[]>([]);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);
  readonly statusFilter = signal<ApplicationStatus | 'ALL'>('PENDING');
  readonly busyId = signal<number | null>(null);
  readonly rejectingId = signal<number | null>(null);
  readonly rejectReason = signal('');

  readonly statusLabels = APPLICATION_STATUS_LABELS;

  readonly filteredGroups = computed<PartnerWithApplications[]>(() => {
    const filter = this.statusFilter();
    const apps = this.applications().filter((a) => filter === 'ALL' || a.status === filter);
    const grouped = new Map<number, PartnerWithApplications>();
    for (const partner of this.partners()) {
      if (partner.id == null) continue;
      grouped.set(partner.id, { partner, applications: [] });
    }
    for (const app of apps) {
      const entry = grouped.get(app.partnerId);
      if (entry) entry.applications.push(app);
    }
    return Array.from(grouped.values()).filter((g) => g.applications.length > 0);
  });

  constructor() {
    this.load();
  }

  setStatusFilter(value: string): void {
    this.statusFilter.set(value as ApplicationStatus | 'ALL');
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

  async approve(app: PartnerApplication): Promise<void> {
    const ok = await this.confirmDialog.ask({
      title: 'Bewerbung annehmen',
      message: `${app.userFirstName} ${app.userLastName} dem Betrieb ${app.partnerName} zuordnen?`,
      confirmLabel: 'Annehmen',
      tone: 'primary',
    });
    if (!ok) return;
    this.busyId.set(app.id);
    this.applicationsService.approve(app.id).subscribe({
      next: () => {
        this.busyId.set(null);
        this.refreshApplications();
      },
      error: () => {
        this.busyId.set(null);
        this.errorMessage.set('Annahme fehlgeschlagen.');
      },
    });
  }

  startReject(app: PartnerApplication): void {
    this.rejectingId.set(app.id);
    this.rejectReason.set('');
  }

  cancelReject(): void {
    this.rejectingId.set(null);
    this.rejectReason.set('');
  }

  confirmReject(app: PartnerApplication): void {
    this.busyId.set(app.id);
    const reason = this.rejectReason().trim();
    this.applicationsService.reject(app.id, reason.length > 0 ? reason : null).subscribe({
      next: () => {
        this.busyId.set(null);
        this.cancelReject();
        this.refreshApplications();
      },
      error: () => {
        this.busyId.set(null);
        this.errorMessage.set('Ablehnung fehlgeschlagen.');
      },
    });
  }

  private load(): void {
    this.loading.set(true);
    this.errorMessage.set(null);
    forkJoin({
      partners: this.partnerService.list(false),
    }).subscribe({
      next: ({ partners }) => {
        this.partners.set(partners);
        this.refreshApplications();
      },
      error: () => {
        this.loading.set(false);
        this.errorMessage.set('Daten konnten nicht geladen werden.');
      },
    });
  }

  private refreshApplications(): void {
    const partners = this.partners();
    if (partners.length === 0) {
      this.applications.set([]);
      this.loading.set(false);
      return;
    }
    const partnerIds = partners.map((p) => p.id).filter((id): id is number => id != null);
    if (partnerIds.length === 0) {
      this.applications.set([]);
      this.loading.set(false);
      return;
    }
    forkJoin(
      partnerIds.map((id) => this.applicationsService.listForPartner(id)),
    ).subscribe({
      next: (lists) => {
        this.applications.set(lists.flat());
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.errorMessage.set('Bewerbungen konnten nicht geladen werden.');
      },
    });
  }
}
