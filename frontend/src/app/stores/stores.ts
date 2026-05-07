import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { PartnerService } from '../partners/partner.service';
import {
  CATEGORY_ICONS,
  CATEGORY_LABELS,
  Category,
  Partner,
  STATUS_LABELS,
  STATUS_ORDER,
  Status,
} from '../partners/partner.model';
import { StoreDetailDialogService } from './store-detail-dialog/store-detail-dialog.service';
import { AuthService } from '../auth/auth.service';

@Component({
  selector: 'app-stores',
  imports: [RouterLink],
  templateUrl: './stores.html',
  styleUrl: './stores.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StoresComponent {
  private readonly service = inject(PartnerService);
  private readonly detailDialog = inject(StoreDetailDialogService);
  private readonly auth = inject(AuthService);

  readonly partners = signal<Partner[]>([]);
  readonly loadError = signal<string | null>(null);
  readonly city = 'Bad Vilbel';
  readonly categoryIcons = CATEGORY_ICONS;
  readonly categoryLabels = CATEGORY_LABELS;
  readonly statusLabels = STATUS_LABELS;

  readonly isRetter = computed(() => !!this.auth.currentUser()?.roles?.includes('RETTER'));
  readonly showOnlyMine = signal(this.isRetter());

  readonly searchTerm = signal('');
  readonly categoryFilter = signal<Category | 'ALL'>('ALL');
  readonly statusFilter = signal<Status | 'ALL'>('ALL');

  readonly categoryOptions: ReadonlyArray<{ value: Category; label: string }> = (
    Object.keys(CATEGORY_LABELS) as Category[]
  ).map((value) => ({ value, label: CATEGORY_LABELS[value] }));
  readonly statusOptions: ReadonlyArray<{ value: Status; label: string }> = STATUS_ORDER.map(
    (value) => ({ value, label: STATUS_LABELS[value] }),
  );

  readonly hasActiveFilters = computed(
    () =>
      this.searchTerm().trim().length > 0 ||
      this.categoryFilter() !== 'ALL' ||
      this.statusFilter() !== 'ALL',
  );

  readonly filteredPartners = computed(() => {
    const term = this.searchTerm().trim().toLowerCase();
    const category = this.categoryFilter();
    const status = this.statusFilter();
    return this.partners().filter((p) => {
      if (category !== 'ALL' && p.category !== category) return false;
      if (status !== 'ALL' && p.status !== status) return false;
      if (term.length > 0) {
        const haystack = `${p.name} ${p.street} ${p.postalCode} ${p.city}`.toLowerCase();
        if (!haystack.includes(term)) return false;
      }
      return true;
    });
  });

  constructor() {
    this.loadPartners();
  }

  onSearchInput(event: Event): void {
    this.searchTerm.set((event.target as HTMLInputElement).value);
  }

  onCategoryChange(event: Event): void {
    this.categoryFilter.set((event.target as HTMLSelectElement).value as Category | 'ALL');
  }

  onStatusChange(event: Event): void {
    this.statusFilter.set((event.target as HTMLSelectElement).value as Status | 'ALL');
  }

  resetFilters(): void {
    this.searchTerm.set('');
    this.categoryFilter.set('ALL');
    this.statusFilter.set('ALL');
  }

  toggleMineFilter(): void {
    this.showOnlyMine.update((v) => !v);
    this.loadPartners();
  }

  private loadPartners(): void {
    this.loadError.set(null);
    this.service.list(this.showOnlyMine()).subscribe({
      next: (list) => this.partners.set(list),
      error: () => this.loadError.set('Betriebe konnten nicht geladen werden.'),
    });
  }

  activeSlotsCount(partner: Partner): number {
    return partner.pickupSlots.filter((s) => s.active).length;
  }

  availableRettersCount(partner: Partner): number {
    return partner.pickupSlots
      .filter((s) => s.active)
      .reduce((sum, s) => sum + (s.availableMemberCount ?? 0), 0);
  }

  openDetail(partner: Partner): void {
    this.detailDialog.open(partner);
  }

  totalCapacity(partner: Partner): number {
    return partner.pickupSlots
      .filter((s) => s.active)
      .reduce((sum, s) => sum + (s.capacity ?? 0), 0);
  }
}
