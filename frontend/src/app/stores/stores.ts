import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { PartnerService } from '../partners/partner.service';
import { CATEGORY_ICONS, CATEGORY_LABELS, Partner } from '../partners/partner.model';
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

  readonly isRetter = computed(() => !!this.auth.currentUser()?.roles?.includes('RETTER'));
  readonly showOnlyMine = signal(this.isRetter());

  readonly mapImage =
    'https://lh3.googleusercontent.com/aida-public/AB6AXuAMqXeSyG_8qK9d_1xvLbIwbKbGx8Kt_G4BsEqUYmOLyqDSpDWCLGu_6ZSf3Q3TIL3T2aaJmaiHgC4CsYRjsQKc4iDg4_RK-1T4sK8xlbqP7mCNr3DbPNyl1-5JBg_qWehzQQCIG9W39xhnXjy3IiCVrmqUzwooBQzG1f_jPzqrOOLJ4BfOMn565V3PsrDMdU36MN4OWaxMsCL5UBhl72rrQT78UIqzIY6KgAPyZkHRzEOUNHWlmIQHTs1_O6QOgsvLm0LtQjy3cQYR';

  constructor() {
    this.loadPartners();
  }

  toggleMineFilter(): void {
    this.showOnlyMine.update((v) => !v);
    this.loadPartners();
  }

  private loadPartners(): void {
    this.loadError.set(null);
    this.service.list(this.showOnlyMine()).subscribe({
      next: (list) => this.partners.set(list),
      error: () => this.loadError.set('Partner konnten nicht geladen werden.'),
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
