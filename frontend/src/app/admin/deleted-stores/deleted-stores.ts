import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Partner } from '../../partners/partner.model';
import { PartnerService } from '../../partners/partner.service';
import { PartnerCategoryRegistry } from '../../partners/partner-category-registry.service';

@Component({
  selector: 'app-deleted-stores',
  imports: [RouterLink],
  templateUrl: './deleted-stores.html',
  styleUrl: './deleted-stores.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DeletedStoresComponent {
  private readonly service = inject(PartnerService);
  private readonly categoryRegistry = inject(PartnerCategoryRegistry);

  readonly partners = signal<Partner[]>([]);
  readonly loadError = signal<string | null>(null);
  readonly actionError = signal<string | null>(null);

  categoryIcon(id: number | null): string {
    return this.categoryRegistry.iconForId(id);
  }
  categoryLabel(id: number | null): string {
    return this.categoryRegistry.labelForId(id);
  }

  constructor() {
    this.reload();
  }

  private reload(): void {
    this.service.listDeleted().subscribe({
      next: (list) => this.partners.set(list),
      error: () => this.loadError.set('Geschlossene Betriebe konnten nicht geladen werden.'),
    });
  }

  restore(partner: Partner): void {
    if (partner.id == null) return;
    this.actionError.set(null);
    this.service.restore(partner.id).subscribe({
      next: () => this.partners.update((list) => list.filter((p) => p.id !== partner.id)),
      error: () => this.actionError.set('Wiederherstellen fehlgeschlagen.'),
    });
  }
}
