import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  ElementRef,
  OnInit,
  ViewChild,
  computed,
  effect,
  inject,
  signal,
} from '@angular/core';
import QRCode from 'qrcode';
import { LiveClockComponent } from './live-clock.component';
import { RescuerCardService } from './rescuer-card.service';
import { RescuerCardContext } from './rescuer-card.model';
import { resolvePhotoUrl } from '../users/photo-url';
import { environment } from '../../environments/environment';

const QR_REFRESH_INTERVAL_MS = 30_000;

@Component({
  selector: 'app-rescuer-card',
  standalone: true,
  imports: [LiveClockComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './rescuer-card.component.html',
})
export class RescuerCardComponent implements OnInit {
  private readonly service = inject(RescuerCardService);
  private readonly destroyRef = inject(DestroyRef);

  @ViewChild('qrCanvas', { static: false }) qrCanvas?: ElementRef<HTMLCanvasElement>;

  readonly context = signal<RescuerCardContext | null>(null);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  readonly fullName = computed(() => {
    const c = this.context();
    if (!c) return '';
    return `${c.firstName} ${c.lastName}`.trim();
  });

  readonly photoSrc = computed(() => resolvePhotoUrl(this.context()?.photoUrl ?? null));

  readonly hygieneExpiryFormatted = computed(() => {
    const c = this.context();
    if (!c?.hygieneExpiryDate) return null;
    const d = new Date(c.hygieneExpiryDate);
    return `${pad(d.getDate())}.${pad(d.getMonth() + 1)}.${d.getFullYear()}`;
  });

  readonly memberSinceFormatted = computed(() => {
    const c = this.context();
    const iso = c?.introductionCompletedAt;
    if (!iso) return null;
    const d = new Date(iso);
    return `${pad(d.getMonth() + 1)}/${(d.getFullYear() % 100).toString().padStart(2, '0')}`;
  });

  readonly partnerLabel = computed(() => {
    const p = this.context()?.currentPickup;
    if (!p) return null;
    return p.partnerName;
  });

  readonly pickupActive = computed(() => this.context()?.currentPickup?.active ?? false);
  readonly hygieneValid = computed(() => this.context()?.hygieneValid ?? false);

  ngOnInit(): void {
    this.loadContext();
    this.startQrRefresh();
  }

  reload(): void {
    this.loadContext();
    this.refreshQr();
  }

  private loadContext(): void {
    this.loading.set(true);
    this.error.set(null);
    this.service.getContext().subscribe({
      next: (ctx) => {
        this.context.set(ctx);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Ausweis konnte nicht geladen werden.');
        this.loading.set(false);
      },
    });
  }

  private startQrRefresh(): void {
    this.refreshQr();
    const handle = setInterval(() => this.refreshQr(), QR_REFRESH_INTERVAL_MS);
    this.destroyRef.onDestroy(() => clearInterval(handle));
  }

  private refreshQr(): void {
    this.service.issueToken().subscribe({
      next: (resp) => {
        const url = `${verifyBaseUrl()}/verify/${encodeURIComponent(resp.token)}`;
        this.drawQr(url);
      },
      error: () => {
        /* QR-Refresh-Fehler werden ignoriert; bestehender QR bleibt sichtbar. */
      },
    });
  }

  private drawQr(text: string): void {
    const canvas = this.qrCanvas?.nativeElement;
    if (!canvas) {
      setTimeout(() => this.drawQr(text), 50);
      return;
    }
    QRCode.toCanvas(canvas, text, {
      width: 128,
      margin: 1,
      color: { dark: '#000000', light: '#ffffff' },
    }).catch(() => {
      /* Render-Fehler ignorieren */
    });
  }
}

function pad(n: number): string {
  return n.toString().padStart(2, '0');
}

function verifyBaseUrl(): string {
  // QR-Code zeigt auf das Frontend, das den Verify-Endpoint aufruft.
  if (typeof window !== 'undefined') {
    return `${window.location.origin}`;
  }
  return environment.apiBaseUrl;
}
