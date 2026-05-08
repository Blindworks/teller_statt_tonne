import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  SystemLogCategory,
  SystemLogEntry,
  SystemLogEventType,
  SystemLogFilter,
  SystemLogSeverity,
  SYSTEM_LOG_CATEGORY_LABELS,
  SYSTEM_LOG_EVENT_TYPE_LABELS,
  SYSTEM_LOG_SEVERITY_LABELS,
} from './system-log.model';
import { SystemLogService } from './system-log.service';

@Component({
  selector: 'app-admin-system-log',
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './admin-system-log.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AdminSystemLogComponent {
  private readonly service = inject(SystemLogService);

  readonly entries = signal<SystemLogEntry[]>([]);
  readonly totalElements = signal(0);
  readonly totalPages = signal(0);
  readonly page = signal(0);
  readonly size = signal(50);
  readonly loading = signal(false);
  readonly loadError = signal<string | null>(null);
  readonly expandedId = signal<number | null>(null);

  readonly availableCategories = signal<SystemLogCategory[]>([]);
  readonly availableSeverities = signal<SystemLogSeverity[]>([]);
  readonly availableEventTypes = signal<SystemLogEventType[]>([]);

  readonly categoryLabels = SYSTEM_LOG_CATEGORY_LABELS;
  readonly severityLabels = SYSTEM_LOG_SEVERITY_LABELS;
  readonly eventTypeLabels = SYSTEM_LOG_EVENT_TYPE_LABELS;

  readonly filterCategory = signal<SystemLogCategory | ''>('');
  readonly filterEventType = signal<SystemLogEventType | ''>('');
  readonly filterSeverity = signal<SystemLogSeverity | ''>('');
  readonly filterFrom = signal<string>('');
  readonly filterTo = signal<string>('');
  readonly filterSearch = signal<string>('');

  readonly hasNext = computed(() => this.page() + 1 < this.totalPages());
  readonly hasPrev = computed(() => this.page() > 0);

  constructor() {
    this.service
      .metadata()
      .pipe(takeUntilDestroyed())
      .subscribe({
        next: (meta) => {
          this.availableCategories.set(meta.categories);
          this.availableSeverities.set(meta.severities);
          this.availableEventTypes.set(meta.eventTypes);
        },
        error: () => {
          // Fallback: alle bekannten Werte aus dem Modell
          this.availableCategories.set(Object.keys(SYSTEM_LOG_CATEGORY_LABELS) as SystemLogCategory[]);
          this.availableSeverities.set(Object.keys(SYSTEM_LOG_SEVERITY_LABELS) as SystemLogSeverity[]);
          this.availableEventTypes.set(Object.keys(SYSTEM_LOG_EVENT_TYPE_LABELS) as SystemLogEventType[]);
        },
      });
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    this.loadError.set(null);
    const filter: SystemLogFilter = {
      category: this.filterCategory() || null,
      eventType: this.filterEventType() || null,
      severity: this.filterSeverity() || null,
      from: this.filterFrom() ? new Date(this.filterFrom()).toISOString() : null,
      to: this.filterTo() ? new Date(this.filterTo() + 'T23:59:59').toISOString() : null,
      search: this.filterSearch() || null,
    };
    this.service.list(filter, this.page(), this.size()).subscribe({
      next: (page) => {
        this.entries.set(page.content);
        this.totalElements.set(page.totalElements);
        this.totalPages.set(page.totalPages);
        this.loading.set(false);
      },
      error: () => {
        this.loadError.set('Systemlog konnte nicht geladen werden.');
        this.loading.set(false);
      },
    });
  }

  applyFilter(): void {
    this.page.set(0);
    this.expandedId.set(null);
    this.reload();
  }

  resetFilter(): void {
    this.filterCategory.set('');
    this.filterEventType.set('');
    this.filterSeverity.set('');
    this.filterFrom.set('');
    this.filterTo.set('');
    this.filterSearch.set('');
    this.applyFilter();
  }

  next(): void {
    if (!this.hasNext()) return;
    this.page.update((p) => p + 1);
    this.expandedId.set(null);
    this.reload();
  }

  prev(): void {
    if (!this.hasPrev()) return;
    this.page.update((p) => p - 1);
    this.expandedId.set(null);
    this.reload();
  }

  toggleExpanded(id: number): void {
    this.expandedId.update((current) => (current === id ? null : id));
  }

  severityClass(severity: SystemLogSeverity): string {
    switch (severity) {
      case 'ERROR':
        return 'bg-error-container text-on-error-container';
      case 'WARN':
        return 'bg-tertiary-container/60 text-on-tertiary-container';
      case 'INFO':
      default:
        return 'bg-surface-container text-on-surface-variant';
    }
  }
}
