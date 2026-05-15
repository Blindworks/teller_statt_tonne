import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AppointmentService } from '../appointment.service';
import { Appointment } from '../appointment.model';
import { formatRange } from '../format';

@Component({
  selector: 'app-upcoming-appointments-widget',
  imports: [RouterLink],
  templateUrl: './upcoming-appointments-widget.html',
  styles: [':host { display: block; }'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UpcomingAppointmentsWidgetComponent {
  private readonly service = inject(AppointmentService);

  readonly items = signal<Appointment[]>([]);
  readonly loaded = signal(false);

  constructor() {
    this.service
      .list(true)
      .pipe(takeUntilDestroyed())
      .subscribe({
        next: (items) => {
          this.items.set(items.slice(0, 5));
          this.loaded.set(true);
        },
        error: () => this.loaded.set(true),
      });
  }

  rangeOf(item: Appointment): string {
    return formatRange(item.startTime, item.endTime);
  }
}
