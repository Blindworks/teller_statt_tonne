import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../auth/auth.service';
import { HygieneCertificate } from '../hygiene-certificate.model';
import { HygieneCertificateService } from '../hygiene-certificate.service';

@Component({
  selector: 'app-hygiene-expiry-banner',
  standalone: true,
  imports: [DatePipe, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (banner(); as info) {
      <div
        class="rounded-xl px-5 py-4 flex flex-wrap items-center gap-3"
        [class.bg-tertiary-container]="info.variant === 'expiring-soon'"
        [class.text-on-tertiary-container]="info.variant === 'expiring-soon'"
        [class.bg-error-container]="info.variant === 'expired'"
        [class.text-on-error-container]="info.variant === 'expired'"
      >
        <div class="flex-1 min-w-0 text-sm">
          <p class="font-bold">{{ info.title }}</p>
          <p class="mt-0.5">
            Gültig bis {{ info.cert.expiryDate | date: 'dd.MM.yyyy' }}.
            @if (info.variant === 'expiring-soon' && info.cert.daysUntilExpiry !== null) {
              Noch {{ info.cert.daysUntilExpiry }} Tage.
            }
          </p>
        </div>
        <a
          routerLink="/profile"
          class="px-4 py-2 rounded-full text-sm font-bold bg-surface text-on-surface hover:bg-surface-container-high"
        >
          Neues Zertifikat hochladen
        </a>
      </div>
    }
  `,
})
export class HygieneExpiryBannerComponent implements OnInit {
  private readonly service = inject(HygieneCertificateService);
  private readonly auth = inject(AuthService);

  readonly certificate = signal<HygieneCertificate | null>(null);

  readonly banner = computed(() => {
    const cert = this.certificate();
    if (!cert) return null;
    if (cert.status !== 'APPROVED') return null;
    if (cert.validityStatus === 'expired') {
      return {
        cert,
        variant: 'expired' as const,
        title: 'Hygienezertifikat abgelaufen — du kannst dich nicht für Pickups eintragen.',
      };
    }
    if (cert.validityStatus === 'expiring-soon') {
      return {
        cert,
        variant: 'expiring-soon' as const,
        title: 'Hygienezertifikat läuft bald ab.',
      };
    }
    return null;
  });

  ngOnInit(): void {
    const userId = this.auth.currentUser()?.id;
    if (userId == null) return;
    this.service.getForUser(userId).subscribe({
      next: (cert) => this.certificate.set(cert),
      error: () => this.certificate.set(null),
    });
  }
}
