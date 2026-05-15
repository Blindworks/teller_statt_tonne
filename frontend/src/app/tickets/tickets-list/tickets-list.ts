import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { TicketService } from '../ticket.service';
import {
  TICKET_CATEGORY_LABEL,
  TICKET_STATUS_LABEL,
  TicketCategory,
  TicketStatus,
  TicketSummary,
} from '../ticket.model';

@Component({
  selector: 'app-tickets-list',
  imports: [RouterLink],
  templateUrl: './tickets-list.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TicketsListComponent {
  private readonly service = inject(TicketService);

  readonly items = signal<TicketSummary[]>([]);
  readonly loadError = signal<string | null>(null);
  readonly statusFilter = signal<TicketStatus | 'ALL'>('ALL');
  readonly categoryFilter = signal<TicketCategory | 'ALL'>('ALL');

  readonly statuses: TicketStatus[] = ['OPEN', 'IN_PROGRESS', 'DONE', 'REJECTED'];
  readonly categories: TicketCategory[] = ['BUG', 'FEATURE'];

  readonly filtered = computed(() => this.items());

  constructor() {
    this.reload();
  }

  reload(): void {
    const filter: { status?: TicketStatus; category?: TicketCategory } = {};
    if (this.statusFilter() !== 'ALL') filter.status = this.statusFilter() as TicketStatus;
    if (this.categoryFilter() !== 'ALL') filter.category = this.categoryFilter() as TicketCategory;
    this.service
      .list(filter)
      .pipe(takeUntilDestroyed())
      .subscribe({
        next: (items) => {
          this.items.set(items);
          this.loadError.set(null);
        },
        error: () => this.loadError.set('Tickets konnten nicht geladen werden.'),
      });
  }

  setStatus(status: TicketStatus | 'ALL'): void {
    this.statusFilter.set(status);
    this.reload();
  }

  setCategory(category: TicketCategory | 'ALL'): void {
    this.categoryFilter.set(category);
    this.reload();
  }

  statusLabel(status: TicketStatus): string {
    return TICKET_STATUS_LABEL[status];
  }

  categoryLabel(category: TicketCategory): string {
    return TICKET_CATEGORY_LABEL[category];
  }

  statusBadgeClass(status: TicketStatus): string {
    switch (status) {
      case 'OPEN':
        return 'bg-blue-100 text-blue-800';
      case 'IN_PROGRESS':
        return 'bg-amber-100 text-amber-800';
      case 'DONE':
        return 'bg-emerald-100 text-emerald-800';
      case 'REJECTED':
        return 'bg-zinc-200 text-zinc-700';
    }
  }

  categoryBadgeClass(category: TicketCategory): string {
    return category === 'BUG'
      ? 'bg-rose-100 text-rose-800'
      : 'bg-violet-100 text-violet-800';
  }

  formatDate(iso: string): string {
    try {
      return new Date(iso).toLocaleString('de-DE', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      });
    } catch {
      return iso;
    }
  }
}
