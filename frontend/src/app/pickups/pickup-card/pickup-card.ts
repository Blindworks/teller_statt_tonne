import { ChangeDetectionStrategy, Component, computed, inject, input, output } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CATEGORY_LABELS, Category } from '../../partners/partner.model';
import { resolvePhotoUrl } from '../../users/photo-url';
import { UserProfileDialogService } from '../../users/user-profile-dialog/user-profile-dialog.service';
import { Pickup } from '../pickup.model';

type Variant = 'FILLED' | 'PARTIAL' | 'EMPTY' | 'COMPLETED' | 'CANCELLED';

export type PickupCardMode = 'PLANNER' | 'RETTER';

@Component({
  selector: 'app-pickup-card',
  imports: [RouterLink],
  templateUrl: './pickup-card.html',
  styleUrl: './pickup-card.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PickupCardComponent {
  private readonly profileDialog = inject(UserProfileDialogService);

  readonly pickup = input.required<Pickup>();
  readonly today = input<boolean>(false);
  readonly mode = input<PickupCardMode>('PLANNER');
  readonly currentUserId = input<number | null>(null);

  readonly signupRequested = output<number>();
  readonly unassignRequested = output<number>();

  readonly categoryLabels = CATEGORY_LABELS;

  readonly variant = computed<Variant>(() => {
    const p = this.pickup();
    if (p.status === 'CANCELLED') return 'CANCELLED';
    if (p.status === 'COMPLETED') return 'COMPLETED';
    if (p.assignments.length >= p.capacity) return 'FILLED';
    if (p.assignments.length > 0) return 'PARTIAL';
    return 'EMPTY';
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

  readonly isRetter = computed(() => this.mode() === 'RETTER');

  readonly currentUserAssigned = computed(() => {
    const uid = this.currentUserId();
    if (uid == null) return false;
    return this.pickup().assignments.some((a) => a.memberId === uid);
  });

  readonly isPast = computed(() => {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const d = new Date(`${this.pickup().date}T00:00:00`);
    return d.getTime() < today.getTime();
  });

  readonly canSignup = computed(() => {
    const p = this.pickup();
    return (
      this.isRetter() &&
      p.status === 'SCHEDULED' &&
      !this.isPast() &&
      !this.currentUserAssigned() &&
      p.assignments.length < p.capacity
    );
  });

  readonly canUnassign = computed(() => {
    const p = this.pickup();
    return (
      this.isRetter() && p.status === 'SCHEDULED' && !this.isPast() && this.currentUserAssigned()
    );
  });

  readonly editLink = computed<unknown[] | null>(() => {
    if (this.isRetter()) return null;
    const id = this.pickup().id;
    return id ? ['/pickups/edit', id] : null;
  });

  avatarUrl(url: string | null): string | null {
    return resolvePhotoUrl(url);
  }

  onAvatarClick(event: MouseEvent, memberId: number): void {
    event.preventDefault();
    event.stopPropagation();
    this.profileDialog.open(memberId);
  }

  onSignup(event: MouseEvent): void {
    event.preventDefault();
    event.stopPropagation();
    const id = this.pickup().id;
    if (id != null) this.signupRequested.emit(id);
  }

  onUnassign(event: MouseEvent): void {
    event.preventDefault();
    event.stopPropagation();
    const id = this.pickup().id;
    if (id != null) this.unassignRequested.emit(id);
  }
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
