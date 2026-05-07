import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { StatsOverview, StatsService } from './stats.service';

@Component({
  selector: 'app-stats-overview',
  standalone: true,
  imports: [DecimalPipe],
  templateUrl: './stats-overview.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StatsOverviewComponent implements OnInit {
  private readonly service = inject(StatsService);

  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);
  readonly data = signal<StatsOverview | null>(null);

  ngOnInit(): void {
    this.service.overview().subscribe({
      next: (overview) => {
        this.data.set(overview);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('Statistik konnte nicht geladen werden.');
        this.loading.set(false);
      },
    });
  }
}
