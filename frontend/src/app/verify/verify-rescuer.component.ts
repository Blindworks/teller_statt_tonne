import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { LiveClockComponent } from '../rescuer-card/live-clock.component';
import { RescuerCardService } from '../rescuer-card/rescuer-card.service';
import { VerifyRescuerResponse } from '../rescuer-card/rescuer-card.model';
import { resolvePhotoUrl } from '../users/photo-url';

@Component({
  selector: 'app-verify-rescuer',
  standalone: true,
  imports: [LiveClockComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="min-h-screen bg-surface-container-low flex items-center justify-center p-4">
      <div class="w-full max-w-md">
        @if (loading()) {
          <p class="text-center text-on-surface-variant">Prüfe Ausweis ...</p>
        } @else if (result(); as r) {
          @if (r.valid) {
            <div class="rounded-3xl overflow-hidden shadow-2xl bg-surface">
              <div class="bg-primary text-on-primary p-6 text-center">
                <span class="material-symbols-outlined text-5xl">verified_user</span>
                <p class="text-xl font-extrabold mt-2">Ausweis gültig</p>
              </div>
              <div class="p-6 flex flex-col items-center gap-4">
                @if (resolvedPhoto(r.photoUrl); as src) {
                  <img [src]="src" alt="Foto" class="w-32 h-32 rounded-full object-cover" />
                }
                <p class="text-2xl font-bold text-on-surface">
                  {{ r.firstName }} {{ r.lastName }}
                </p>
                @if (r.hygieneValid) {
                  <p class="text-sm text-primary font-bold">Hygiene-Zertifikat gültig</p>
                } @else {
                  <p class="text-sm text-error font-extrabold">Hygiene-Zertifikat ungültig</p>
                }
                @if (r.currentPartnerName) {
                  <div class="text-center">
                    <p class="text-xs text-on-surface-variant uppercase tracking-wider">
                      {{ r.pickupActive ? 'Gerade aktiv bei' : 'Nächster Einsatz' }}
                    </p>
                    <p class="font-bold">{{ r.currentPartnerName }}</p>
                  </div>
                } @else {
                  <p class="text-sm text-on-surface-variant italic">Kein Einsatz heute</p>
                }
                <div class="mt-2 text-center">
                  <p class="text-xs text-on-surface-variant">Aktuelle Zeit</p>
                  <p class="text-2xl font-mono font-bold"><app-live-clock /></p>
                </div>
              </div>
            </div>
          } @else {
            <div class="rounded-3xl overflow-hidden shadow-2xl bg-surface">
              <div class="bg-error text-on-error p-6 text-center">
                <span class="material-symbols-outlined text-5xl">cancel</span>
                <p class="text-xl font-extrabold mt-2">Ausweis ungültig</p>
                <p class="text-sm mt-1 opacity-90">{{ reasonLabel(r.reason) }}</p>
              </div>
              <div class="p-6">
                <p class="text-sm text-on-surface-variant text-center">
                  Bitte den Retter um einen frisch generierten QR-Code (alle 30 Sekunden neu).
                </p>
              </div>
            </div>
          }
        }
      </div>
    </div>
  `,
})
export class VerifyRescuerComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly service = inject(RescuerCardService);

  readonly loading = signal(true);
  readonly result = signal<VerifyRescuerResponse | null>(null);

  ngOnInit(): void {
    const token = this.route.snapshot.paramMap.get('token') ?? '';
    if (!token) {
      this.result.set({
        valid: false,
        reason: 'missing',
        firstName: null,
        lastName: null,
        photoUrl: null,
        hygieneValid: false,
        currentPartnerName: null,
        pickupActive: false,
        generatedAt: null,
      });
      this.loading.set(false);
      return;
    }
    this.service.verify(token).subscribe({
      next: (r) => {
        this.result.set(r);
        this.loading.set(false);
      },
      error: () => {
        this.result.set({
          valid: false,
          reason: 'network',
          firstName: null,
          lastName: null,
          photoUrl: null,
          hygieneValid: false,
          currentPartnerName: null,
          pickupActive: false,
          generatedAt: null,
        });
        this.loading.set(false);
      },
    });
  }

  resolvedPhoto(url: string | null): string | null {
    return resolvePhotoUrl(url);
  }

  reasonLabel(reason: string | null): string {
    switch (reason) {
      case 'expired':
        return 'Der QR-Code ist abgelaufen.';
      case 'invalid':
        return 'Der QR-Code ist ungültig.';
      case 'user-not-found':
        return 'Nutzer existiert nicht (mehr).';
      case 'missing':
        return 'Kein Token im Link.';
      case 'network':
        return 'Verbindungsfehler — bitte erneut scannen.';
      default:
        return 'Ausweis konnte nicht verifiziert werden.';
    }
  }
}
