import { ChangeDetectionStrategy, Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormBuilder, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import {
  HYGIENE_CERTIFICATE_STATUS_LABELS,
  HygieneCertificate,
} from '../hygiene-certificate.model';
import { HygieneCertificateService } from '../hygiene-certificate.service';

const MAX_BYTES = 10 * 1024 * 1024;
const ALLOWED_MIME = new Set([
  'application/pdf',
  'image/jpeg',
  'image/png',
  'image/webp',
]);

type UploadForm = FormGroup<{
  issuedDate: FormControl<string>;
}>;

@Component({
  selector: 'app-hygiene-certificate-section',
  standalone: true,
  imports: [DatePipe, ReactiveFormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './hygiene-certificate-section.component.html',
})
export class HygieneCertificateSectionComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly service = inject(HygieneCertificateService);

  readonly userId = input.required<number>();
  readonly hasRetterRole = input<boolean>(false);
  readonly compact = input<boolean>(false);

  readonly showUploadForm = signal(false);

  readonly certificate = signal<HygieneCertificate | null>(null);
  readonly loading = signal(true);
  readonly uploading = signal(false);
  readonly error = signal<string | null>(null);
  readonly message = signal<string | null>(null);
  readonly previewUrl = signal<string | null>(null);
  readonly previewLoading = signal(false);
  readonly selectedFile = signal<File | null>(null);
  readonly selectedFileName = computed(() => this.selectedFile()?.name ?? null);
  readonly statusLabels = HYGIENE_CERTIFICATE_STATUS_LABELS;
  readonly today = new Date().toISOString().slice(0, 10);

  readonly uploadForm: UploadForm = this.fb.group({
    issuedDate: this.fb.nonNullable.control('', Validators.required),
  });

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.service.getForUser(this.userId()).subscribe({
      next: (cert) => {
        this.certificate.set(cert);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.error.set('Zertifikat konnte nicht geladen werden.');
      },
    });
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    this.error.set(null);
    if (!file) {
      this.selectedFile.set(null);
      return;
    }
    if (!ALLOWED_MIME.has(file.type)) {
      this.error.set('Nur PDF, JPG, PNG oder WebP erlaubt.');
      input.value = '';
      this.selectedFile.set(null);
      return;
    }
    if (file.size > MAX_BYTES) {
      this.error.set('Datei ist zu groß (max. 10 MB).');
      input.value = '';
      this.selectedFile.set(null);
      return;
    }
    this.selectedFile.set(file);
  }

  submit(): void {
    if (this.uploadForm.invalid) {
      this.uploadForm.markAllAsTouched();
      return;
    }
    const file = this.selectedFile();
    if (!file) {
      this.error.set('Bitte eine Datei auswählen.');
      return;
    }
    const issuedDate = this.uploadForm.controls.issuedDate.value;
    this.uploading.set(true);
    this.error.set(null);
    this.message.set(null);
    this.service.upload(this.userId(), file, issuedDate).subscribe({
      next: (cert) => {
        this.uploading.set(false);
        this.certificate.set(cert);
        this.selectedFile.set(null);
        this.uploadForm.reset({ issuedDate: '' });
        this.message.set('Zertifikat hochgeladen — wartet auf Prüfung.');
        this.revokePreview();
      },
      error: (err) => {
        this.uploading.set(false);
        this.error.set(typeof err?.error === 'string' ? err.error : 'Upload fehlgeschlagen.');
      },
    });
  }

  openPreview(): void {
    const existing = this.previewUrl();
    if (existing) {
      window.open(existing, '_blank', 'noopener');
      return;
    }
    this.previewLoading.set(true);
    this.service.fetchFile(this.userId()).subscribe({
      next: (blob) => {
        this.previewLoading.set(false);
        const url = URL.createObjectURL(blob);
        this.previewUrl.set(url);
        window.open(url, '_blank', 'noopener');
      },
      error: () => {
        this.previewLoading.set(false);
        this.error.set('Vorschau konnte nicht geladen werden.');
      },
    });
  }

  badgeClass(status: string): string {
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

  private revokePreview(): void {
    const url = this.previewUrl();
    if (url) {
      URL.revokeObjectURL(url);
      this.previewUrl.set(null);
    }
  }
}
