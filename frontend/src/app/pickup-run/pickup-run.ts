import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { AuthService } from '../auth/auth.service';
import { ConfirmDialogService } from '../shared/confirm-dialog/confirm-dialog.service';
import { FoodCategoryService } from '../food-categories/food-category.service';
import { FoodCategory } from '../food-categories/food-category.model';
import { PartnerService } from '../partners/partner.service';
import { Partner } from '../partners/partner.model';
import { PickupService } from '../pickups/pickup.service';
import { Pickup } from '../pickups/pickup.model';
import { DistributionPointService } from '../admin/distribution-points/distribution-point.service';
import { DistributionPoint } from '../admin/distribution-points/distribution-point.model';
import { PickupRunService } from './pickup-run.service';
import { PickupRun } from './pickup-run.model';

type Step = 'info' | 'items' | 'distribution';

const LAST_DP_KEY = 'tst.lastDistributionPointId';

@Component({
  selector: 'app-pickup-run',
  imports: [CommonModule, FormsModule],
  templateUrl: './pickup-run.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PickupRunComponent {
  private readonly route = inject(ActivatedRoute);
  readonly router = inject(Router);
  private readonly runService = inject(PickupRunService);
  private readonly pickupService = inject(PickupService);
  private readonly partnerService = inject(PartnerService);
  private readonly foodCategoryService = inject(FoodCategoryService);
  private readonly dpService = inject(DistributionPointService);
  private readonly auth = inject(AuthService);
  private readonly confirmDialog = inject(ConfirmDialogService);

  readonly pickupId = signal<number | null>(null);
  readonly step = signal<Step>('info');
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);

  readonly pickup = signal<Pickup | null>(null);
  readonly partner = signal<Partner | null>(null);
  readonly run = signal<PickupRun | null>(null);
  readonly categories = signal<FoodCategory[]>([]);
  readonly distributionPoints = signal<DistributionPoint[]>([]);

  readonly customLabel = signal('');
  readonly selectedDpId = signal<number | null>(null);
  readonly notes = signal('');
  readonly photoQueue = signal<File[]>([]);
  readonly completing = signal(false);

  readonly preferredCategories = computed<FoodCategory[]>(() => {
    const ids = this.partner()?.preferredFoodCategoryIds ?? [];
    const all = this.categories();
    return ids
      .map((id) => all.find((c) => c.id === id))
      .filter((c): c is FoodCategory => !!c && c.active);
  });

  readonly otherCategories = computed<FoodCategory[]>(() => {
    const preferred = new Set(this.partner()?.preferredFoodCategoryIds ?? []);
    return this.categories().filter((c) => c.active && !preferred.has(c.id!));
  });

  readonly itemTags = computed(() => {
    const run = this.run();
    if (!run) return [];
    const catMap = new Map(this.categories().map((c) => [c.id!, c]));
    return run.items.map((i) => ({
      ...i,
      label: i.customLabel ?? catMap.get(i.foodCategoryId ?? -1)?.name ?? 'Unbekannt',
    }));
  });

  readonly customItems = computed(() => this.run()?.items.filter((i) => i.customLabel != null) ?? []);

  private readonly categoryToItemId = computed<Map<number, number>>(() => {
    const map = new Map<number, number>();
    for (const i of this.run()?.items ?? []) {
      if (i.foodCategoryId != null) map.set(i.foodCategoryId, i.id);
    }
    return map;
  });

  isSelected(categoryId: number): boolean {
    return this.categoryToItemId().has(categoryId);
  }

  constructor() {
    const id = Number(this.route.snapshot.paramMap.get('pickupId'));
    if (!id) {
      this.errorMessage.set('Ungültige Abholung.');
      this.loading.set(false);
      return;
    }
    this.pickupId.set(id);
    this.bootstrap(id);
  }

  private async bootstrap(pickupId: number): Promise<void> {
    try {
      const [pickup, run] = await Promise.all([
        firstValueFrom(this.pickupService.get(pickupId)),
        firstValueFrom(this.runService.start(pickupId)),
      ]);
      this.pickup.set(pickup);
      this.run.set(run);

      const [categories, dps] = await Promise.all([
        firstValueFrom(this.foodCategoryService.listActive()),
        firstValueFrom(this.dpService.list()),
      ]);
      this.categories.set(categories);
      this.distributionPoints.set(dps);

      if (pickup.partnerId) {
        const partner = await firstValueFrom(this.partnerService.get(pickup.partnerId));
        this.partner.set(partner);
      }

      this.selectedDpId.set(this.defaultDistributionPointId(dps));
      this.notes.set(run.notes ?? '');
      this.loading.set(false);
    } catch (err: unknown) {
      this.loading.set(false);
      const e = err as { status?: number; error?: unknown };
      if (e.status === 403) {
        this.errorMessage.set('Du bist nicht für diese Abholung eingetragen.');
      } else if (typeof e.error === 'string') {
        this.errorMessage.set(e.error);
      } else {
        this.errorMessage.set('Abholung konnte nicht gestartet werden.');
      }
    }
  }

  private defaultDistributionPointId(dps: DistributionPoint[]): number | null {
    const userId = this.auth.currentUser()?.id;
    if (userId) {
      const own = dps.find((d) => d.operators.some((o) => o.id === userId));
      if (own?.id != null) return own.id;
    }
    const stored = Number(localStorage.getItem(LAST_DP_KEY));
    if (stored && dps.some((d) => d.id === stored)) return stored;
    return dps[0]?.id ?? null;
  }

  setStep(step: Step): void {
    this.step.set(step);
  }

  toggleCategory(category: FoodCategory): void {
    const pickupId = this.pickupId();
    if (!pickupId || category.id == null) return;
    const existingItemId = this.categoryToItemId().get(category.id);
    if (existingItemId != null) {
      this.removeItem(existingItemId);
      return;
    }
    this.runService.addItem(pickupId, { foodCategoryId: category.id }).subscribe({
      next: (item) => {
        this.run.update((r) => (r ? { ...r, items: [...r.items, item] } : r));
      },
      error: () => this.errorMessage.set('Konnte nicht hinzugefügt werden.'),
    });
  }

  addCustom(): void {
    const pickupId = this.pickupId();
    const label = this.customLabel().trim();
    if (!pickupId || !label) return;
    this.runService.addItem(pickupId, { customLabel: label }).subscribe({
      next: (item) => {
        this.run.update((r) => (r ? { ...r, items: [...r.items, item] } : r));
        this.customLabel.set('');
      },
      error: () => this.errorMessage.set('Konnte nicht hinzugefügt werden.'),
    });
  }

  removeItem(itemId: number): void {
    const pickupId = this.pickupId();
    if (!pickupId) return;
    this.runService.removeItem(pickupId, itemId).subscribe({
      next: () => {
        this.run.update((r) => (r ? { ...r, items: r.items.filter((i) => i.id !== itemId) } : r));
      },
    });
  }

  onPhotosSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const files = Array.from(input.files ?? []);
    if (!files.length) return;
    this.photoQueue.update((q) => [...q, ...files]);
    input.value = '';
  }

  removePhoto(index: number): void {
    this.photoQueue.update((q) => q.filter((_, i) => i !== index));
  }

  photoPreview(file: File): string {
    return URL.createObjectURL(file);
  }

  async complete(): Promise<void> {
    const pickupId = this.pickupId();
    const dpId = this.selectedDpId();
    if (!pickupId || dpId == null) {
      this.errorMessage.set('Bitte einen Verteiler wählen.');
      return;
    }
    this.completing.set(true);
    try {
      const result = await firstValueFrom(
        this.runService.complete(pickupId, { distributionPointId: dpId, notes: this.notes() || null }),
      );
      for (const file of this.photoQueue()) {
        try {
          await firstValueFrom(this.runService.addPhoto(result.post.id, file));
        } catch {
          // best-effort; weiter mit den anderen Fotos
        }
      }
      localStorage.setItem(LAST_DP_KEY, String(dpId));
      this.router.navigate(['/dashboard'], { queryParams: { runCompleted: 1 } });
    } catch (err: unknown) {
      this.completing.set(false);
      const e = err as { error?: unknown };
      this.errorMessage.set(typeof e.error === 'string' ? e.error : 'Abschließen fehlgeschlagen.');
    }
  }

  async abort(): Promise<void> {
    const ok = await this.confirmDialog.ask({
      title: 'Abholung abbrechen?',
      message: 'Die Erfassung geht verloren. Möchtest du wirklich abbrechen?',
      confirmLabel: 'Abbrechen',
      cancelLabel: 'Weiter erfassen',
      tone: 'danger',
    });
    if (!ok) return;
    const pickupId = this.pickupId();
    if (!pickupId) return;
    this.runService.abort(pickupId).subscribe({
      next: () => this.router.navigate(['/dashboard']),
    });
  }

  back(): void {
    if (this.step() === 'items') this.step.set('info');
    else if (this.step() === 'distribution') this.step.set('items');
  }

  next(): void {
    if (this.step() === 'info') this.step.set('items');
    else if (this.step() === 'items') this.step.set('distribution');
  }
}
