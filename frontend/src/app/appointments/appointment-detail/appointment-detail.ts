import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AppointmentService } from '../appointment.service';
import { Appointment } from '../appointment.model';
import { formatRange } from '../format';

@Component({
  selector: 'app-appointment-detail',
  imports: [RouterLink],
  templateUrl: './appointment-detail.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AppointmentDetailComponent {
  private readonly service = inject(AppointmentService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly item = signal<Appointment | null>(null);
  readonly loadError = signal<string | null>(null);

  constructor() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!Number.isFinite(id) || id <= 0) {
      this.loadError.set('Ungültige Termin-ID.');
      return;
    }
    this.service
      .get(id)
      .pipe(takeUntilDestroyed())
      .subscribe({
        next: (item) => {
          this.item.set(item);
          if (!item.read) {
            this.service.markRead(item.id).subscribe();
          }
        },
        error: (err) => {
          if (err?.status === 404) {
            this.loadError.set('Termin nicht gefunden.');
          } else {
            this.loadError.set('Termin konnte nicht geladen werden.');
          }
        },
      });
  }

  rangeOf(item: Appointment): string {
    return formatRange(item.startTime, item.endTime);
  }

  rolesLabel(item: Appointment): string {
    const parts = item.targetRoles.map((r) => r.label || r.name);
    if (item.isPublic) parts.unshift('Öffentlich');
    return parts.join(', ') || '–';
  }

  back(): void {
    this.router.navigateByUrl('/termine');
  }
}
