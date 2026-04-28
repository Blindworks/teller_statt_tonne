import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { PartnerService } from '../partners/partner.service';
import { CATEGORY_ICONS, CATEGORY_LABELS, Partner } from '../partners/partner.model';

@Component({
  selector: 'app-stores',
  imports: [RouterLink],
  templateUrl: './stores.html',
  styleUrl: './stores.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StoresComponent {
  private readonly service = inject(PartnerService);

  readonly partners = signal<Partner[]>([]);
  readonly loadError = signal<string | null>(null);
  readonly city = 'Bad Vilbel';
  readonly categoryIcons = CATEGORY_ICONS;
  readonly categoryLabels = CATEGORY_LABELS;

  readonly mapImage =
    'https://lh3.googleusercontent.com/aida-public/AB6AXuAMqXeSyG_8qK9d_1xvLbIwbKbGx8Kt_G4BsEqUYmOLyqDSpDWCLGu_6ZSf3Q3TIL3T2aaJmaiHgC4CsYRjsQKc4iDg4_RK-1T4sK8xlbqP7mCNr3DbPNyl1-5JBg_qWehzQQCIG9W39xhnXjy3IiCVrmqUzwooBQzG1f_jPzqrOOLJ4BfOMn565V3PsrDMdU36MN4OWaxMsCL5UBhl72rrQT78UIqzIY6KgAPyZkHRzEOUNHWlmIQHTs1_O6QOgsvLm0LtQjy3cQYR';

  constructor() {
    this.service.list().subscribe({
      next: (list) => this.partners.set(list),
      error: () => this.loadError.set('Partner konnten nicht geladen werden.'),
    });
  }

  activeSlotsCount(partner: Partner): number {
    return partner.pickupSlots.filter((s) => s.active).length;
  }
}
