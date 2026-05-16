import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { PartnerService } from '../partners/partner.service';
import { PartnerCategoryRegistry } from '../partners/partner-category-registry.service';
import {
  Partner,
  STATUS_LABELS,
  STATUS_ORDER,
  Status,
} from '../partners/partner.model';
import { StoreDetailDialogService } from './store-detail-dialog/store-detail-dialog.service';
import { AuthService } from '../auth/auth.service';
import { ApplyToStoreDialogService } from '../partner-applications/apply-to-store-dialog/apply-to-store-dialog.service';
import { ApplyToStoreDialogComponent } from '../partner-applications/apply-to-store-dialog/apply-to-store-dialog.component';
import { PartnerApplicationsService } from '../partner-applications/partner-applications.service';
import { PartnerApplication } from '../partner-applications/partner-application.model';
import { PhotoUrlPipe } from '../users/photo-url.pipe';

@Component({
  selector: 'app-stores',
  imports: [RouterLink, ApplyToStoreDialogComponent, PhotoUrlPipe],
  templateUrl: './stores.html',
  styleUrl: './stores.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StoresComponent {
  private readonly service = inject(PartnerService);
  private readonly detailDialog = inject(StoreDetailDialogService);
  private readonly auth = inject(AuthService);
  private readonly applyDialog = inject(ApplyToStoreDialogService);
  private readonly applications = inject(PartnerApplicationsService);
  private readonly categoryRegistry = inject(PartnerCategoryRegistry);

  readonly partners = signal<Partner[]>([]);
  readonly memberCounts = signal<Record<number, number>>({});
  readonly loadError = signal<string | null>(null);
  readonly city = 'Bad Vilbel';
  readonly statusLabels = STATUS_LABELS;

  categoryIcon(id: number | null): string {
    return this.categoryRegistry.iconForId(id);
  }
  categoryLabel(id: number | null): string {
    return this.categoryRegistry.labelForId(id);
  }

  readonly isRetter = computed(() => !!this.auth.currentUser()?.roles?.includes('RETTER'));
  readonly canApply = computed(() => {
    const roles = this.auth.currentUser()?.roles ?? [];
    return roles.includes('RETTER') || roles.includes('NEW_MEMBER');
  });
  readonly showOnlyMine = signal(this.isRetter());
  readonly myApplications = signal<PartnerApplication[]>([]);
  readonly myPartnerIds = signal<ReadonlySet<number>>(new Set());
  readonly pendingApplicationPartnerIds = computed(
    () => new Set(this.myApplications().filter((a) => a.status === 'PENDING').map((a) => a.partnerId)),
  );

  readonly searchTerm = signal('');
  readonly categoryFilter = signal<number | 'ALL'>('ALL');
  readonly statusFilter = signal<Status | 'ALL'>('ALL');

  readonly categoryOptions = this.categoryRegistry.categories;
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
      if (category !== 'ALL' && p.categoryId !== category) return false;
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
    this.refreshMyApplications();
    this.refreshMyMemberships();
    this.refreshMemberCounts();
  }

  private refreshMemberCounts(): void {
    this.service.memberCounts().subscribe({
      next: (counts) => this.memberCounts.set(counts),
      error: () => {
        /* silent — kachel zeigt dann 0 */
      },
    });
  }

  hasPendingApplication(partnerId: number): boolean {
    return this.pendingApplicationPartnerIds().has(partnerId);
  }

  isMyMember(partnerId: number): boolean {
    return this.myPartnerIds().has(partnerId);
  }

  openApplyDialog(partner: Partner, event: Event): void {
    event.stopPropagation();
    if (partner.id == null) return;
    this.applyDialog.open({ partnerId: partner.id, partnerName: partner.name });
    setTimeout(() => {
      this.refreshMyApplications();
      this.refreshMyMemberships();
    }, 800);
  }

  private refreshMyApplications(): void {
    if (!this.canApply()) return;
    this.applications.listMine().subscribe({
      next: (apps) => this.myApplications.set(apps),
      error: () => {
        /* silent — dashboard works without */
      },
    });
  }

  private refreshMyMemberships(): void {
    if (!this.canApply()) return;
    this.service.list(true).subscribe({
      next: (list) => {
        const ids = new Set<number>();
        for (const p of list) if (p.id != null) ids.add(p.id);
        this.myPartnerIds.set(ids);
      },
      error: () => {
        /* silent */
      },
    });
  }

  onSearchInput(event: Event): void {
    this.searchTerm.set((event.target as HTMLInputElement).value);
  }

  onCategoryChange(event: Event): void {
    const raw = (event.target as HTMLSelectElement).value;
    this.categoryFilter.set(raw === 'ALL' ? 'ALL' : Number(raw));
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

  assignedRettersCount(partner: Partner): number {
    if (partner.id == null) return 0;
    return this.memberCounts()[partner.id] ?? 0;
  }

  canOpenDetail(partner: Partner): boolean {
    if (!this.isRetter()) return true;
    return partner.id != null && this.isMyMember(partner.id);
  }

  openDetail(partner: Partner): void {
    if (!this.canOpenDetail(partner)) return;
    this.detailDialog.open(partner);
  }

  totalCapacity(partner: Partner): number {
    return partner.pickupSlots
      .filter((s) => s.active)
      .reduce((sum, s) => sum + (s.capacity ?? 0), 0);
  }
}
