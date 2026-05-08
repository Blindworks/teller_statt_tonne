import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { ConfirmDialogService } from '../../shared/confirm-dialog/confirm-dialog.service';
import {
  HYGIENE_CERTIFICATE_STATUS_LABELS,
  HygieneCertificate,
  HygieneCertificateStatus,
} from '../hygiene-certificate.model';
import { HygieneCertificateService } from '../hygiene-certificate.service';

type StatusFilter = HygieneCertificateStatus | 'ALL';

@Component({
  selector: 'app-admin-hygiene-certificates',
  imports: [DatePipe, FormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './admin-hygiene-certificates.component.html',
})
export class AdminHygieneCertificatesComponent {
  private readonly service = inject(HygieneCertificateService);
  private readonly confirm = inject(ConfirmDialogService);
  private readonly sanitizer = inject(DomSanitizer);

  readonly certificates = signal<HygieneCertificate[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly statusFilter = signal<StatusFilter>('PENDING');
  readonly busyId = signal<number | null>(null);
  readonly rejectingId = signal<number | null>(null);
  readonly rejectReason = signal('');
  readonly previewUrl = signal<string | null>(null);
  readonly previewSafeUrl = signal<SafeResourceUrl | null>(null);
  readonly previewMime = signal<string | null>(null);
  readonly previewForCertId = signal<number | null>(null);
  readonly previewLoading = signal(false);

  readonly statusLabels = HYGIENE_CERTIFICATE_STATUS_LABELS;

  readonly filtered = computed(() => {
    const filter = this.statusFilter();
    return filter === 'ALL'
      ? this.certificates()
      : this.certificates().filter((c) => c.status === filter);
  });

  constructor() {
    this.load();
  }

  setStatusFilter(value: string): void {
    this.statusFilter.set(value as StatusFilter);
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    const filter = this.statusFilter();
    const status = filter === 'ALL' ? undefined : filter;
    this.service.list(status).subscribe({
      next: (list) => {
        this.certificates.set(list);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.error.set('Zertifikate konnten nicht geladen werden.');
      },
    });
  }

  badgeClass(status: HygieneCertificateStatus): string {
    switch (status) {
      case 'PENDING':
        return 'bg-tertiary-container text-on-tertiary-container';
      case 'APPROVED':
        return 'bg-primary-container text-on-primary-container';
      case 'REJECTED':
        return 'bg-error-container text-on-error-container';
      default:
        return 'bg-surface-container text-on-surface';
    }
  }

  togglePreview(cert: HygieneCertificate): void {
    if (this.previewForCertId() === cert.id) {
      this.closePreview();
      return;
    }
    this.closePreview();
    this.previewLoading.set(true);
    this.previewForCertId.set(cert.id);
    this.service.fetchFile(cert.userId).subscribe({
      next: (blob) => {
        this.previewLoading.set(false);
        const url = URL.createObjectURL(blob);
        this.previewUrl.set(url);
        this.previewSafeUrl.set(this.sanitizer.bypassSecurityTrustResourceUrl(url));
        this.previewMime.set(cert.mimeType);
      },
      error: () => {
        this.previewLoading.set(false);
        this.previewForCertId.set(null);
        this.error.set('Vorschau konnte nicht geladen werden.');
      },
    });
  }

  isPreviewPdf(): boolean {
    return this.previewMime() === 'application/pdf';
  }

  closePreview(): void {
    const url = this.previewUrl();
    if (url) URL.revokeObjectURL(url);
    this.previewUrl.set(null);
    this.previewSafeUrl.set(null);
    this.previewMime.set(null);
    this.previewForCertId.set(null);
  }

  async approve(cert: HygieneCertificate): Promise<void> {
    const ok = await this.confirm.ask({
      title: 'Zertifikat genehmigen',
      message: `${cert.userFirstName ?? ''} ${cert.userLastName ?? ''} als Retter freischalten?`.trim(),
      confirmLabel: 'Genehmigen',
      tone: 'primary',
    });
    if (!ok) return;
    this.busyId.set(cert.id);
    this.service.approve(cert.id).subscribe({
      next: () => {
        this.busyId.set(null);
        this.closePreview();
        this.load();
      },
      error: () => {
        this.busyId.set(null);
        this.error.set('Genehmigung fehlgeschlagen.');
      },
    });
  }

  startReject(cert: HygieneCertificate): void {
    this.rejectingId.set(cert.id);
    this.rejectReason.set('');
  }

  cancelReject(): void {
    this.rejectingId.set(null);
    this.rejectReason.set('');
  }

  confirmReject(cert: HygieneCertificate): void {
    const reason = this.rejectReason().trim();
    if (!reason) {
      this.error.set('Bitte eine Begründung angeben.');
      return;
    }
    this.busyId.set(cert.id);
    this.service.reject(cert.id, reason).subscribe({
      next: () => {
        this.busyId.set(null);
        this.cancelReject();
        this.closePreview();
        this.load();
      },
      error: (err) => {
        this.busyId.set(null);
        this.error.set(typeof err?.error === 'string' ? err.error : 'Ablehnung fehlgeschlagen.');
      },
    });
  }
}
