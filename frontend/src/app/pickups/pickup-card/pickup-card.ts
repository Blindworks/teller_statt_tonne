import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CATEGORY_LABELS, Category } from '../../partners/partner.model';
import { Pickup } from '../pickup.model';

type Variant = 'FILLED' | 'OPEN' | 'COMPLETED' | 'CANCELLED';

@Component({
  selector: 'app-pickup-card',
  imports: [RouterLink],
  templateUrl: './pickup-card.html',
  styleUrl: './pickup-card.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PickupCardComponent {
  readonly pickup = input.required<Pickup>();
  readonly today = input<boolean>(false);

  readonly categoryLabels = CATEGORY_LABELS;

  readonly variant = computed<Variant>(() => {
    const p = this.pickup();
    if (p.status === 'CANCELLED') return 'CANCELLED';
    if (p.status === 'COMPLETED') return 'COMPLETED';
    return p.assignments.length >= p.capacity ? 'FILLED' : 'OPEN';
  });

  readonly remaining = computed(() => {
    const p = this.pickup();
    return Math.max(0, p.capacity - p.assignments.length);
  });

  readonly chipClasses = computed(() => {
    const c = this.pickup().partnerCategory;
    return chipClass(c);
  });

  readonly chipLabel = computed(() => {
    const c = this.pickup().partnerCategory;
    return c ? CATEGORY_LABELS[c] : 'Partner';
  });
}

function chipClass(category: Category | null): string {
  switch (category) {
    case 'BAKERY':
      return 'text-primary bg-primary-container';
    case 'SUPERMARKET':
      return 'text-secondary bg-secondary-container';
    case 'CAFE':
    case 'RESTAURANT':
      return 'text-tertiary bg-tertiary-container';
    default:
      return 'text-on-surface-variant bg-surface-container-high';
  }
}
