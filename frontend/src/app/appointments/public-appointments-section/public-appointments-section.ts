import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AppointmentService } from '../appointment.service';
import { PublicAppointment } from '../appointment.model';
import { formatRange } from '../format';

@Component({
  selector: 'app-public-appointments-section',
  imports: [],
  templateUrl: './public-appointments-section.html',
  styles: [':host { display: block; }'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PublicAppointmentsSectionComponent {
  private readonly service = inject(AppointmentService);

  readonly items = signal<PublicAppointment[]>([]);
  readonly loaded = signal(false);

  constructor() {
    this.service
      .listPublic()
      .pipe(takeUntilDestroyed())
      .subscribe({
        next: (items) => {
          this.items.set(items);
          this.loaded.set(true);
        },
        error: () => this.loaded.set(true),
      });
  }

  rangeOf(item: PublicAppointment): string {
    return formatRange(item.startTime, item.endTime);
  }

  isUrl(value: string | null): boolean {
    if (!value) return false;
    return /^https?:\/\//i.test(value);
  }
}
