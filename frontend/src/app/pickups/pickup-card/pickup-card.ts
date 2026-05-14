import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  computed,
  inject,
  input,
  output,
  signal,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { PartnerCategoryRegistry } from '../../partners/partner-category-registry.service';
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
  private readonly categoryRegistry = inject(PartnerCategoryRegistry);

  private readonly nowMs = signal(Date.now());

  constructor() {
    const intervalId = setInterval(() => this.nowMs.set(Date.now()), 30_000);
    inject(DestroyRef).onDestroy(() => clearInterval(intervalId));
  }

  readonly pickup = input.required<Pickup>();
  readonly today = input<boolean>(false);
  readonly mode = input<PickupCardMode>('PLANNER');
  readonly currentUserId = input<number | null>(null);

  readonly signupRequested = output<number>();
  readonly unassignRequested = output<number>();

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

  readonly isEvent = computed(() => this.pickup().eventId != null);

  readonly chipClasses = computed(() => {
    if (this.isEvent()) return 'text-on-primary bg-primary';
    const c = this.categoryRegistry.byId(this.pickup().partnerCategoryId);
    return chipClass(c?.code ?? null);
  });

  readonly chipLabel = computed(() => {
    if (this.isEvent()) return 'Sonderabholung';
    const c = this.categoryRegistry.byId(this.pickup().partnerCategoryId);
    return c?.label ?? 'Partner';
  });

  readonly displayTitle = computed(() => {
    const p = this.pickup();
    if (p.eventId != null) return p.eventName ?? 'Sonderabholung';
    return p.partnerName ?? 'Unbekannt';
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

  readonly isWithinUnassignCutoff = computed(() => {
    const p = this.pickup();
    const start = new Date(`${p.date}T${p.startTime}:00`);
    return start.getTime() - this.nowMs() < 2 * 60 * 60 * 1000;
  });

  readonly canUnassign = computed(() => {
    const p = this.pickup();
    return (
      this.isRetter() && p.status === 'SCHEDULED' && !this.isPast() && this.currentUserAssigned()
    );
  });

  readonly unassignDisabled = computed(() => this.isWithinUnassignCutoff());

  readonly unassignDisabledReason =
    'Austragen ist nur bis 2 Stunden vor Beginn möglich.';

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

function chipClass(code: string | null): string {
  switch (code) {
    case 'BAKERY':
      return 'text-primary bg-primary-container';
    case 'SUPERMARKET':
      return 'text-secondary bg-secondary-container';
    case 'CAFE':
    case 'RESTAURANT':
    case 'BUTCHER':
      return 'text-tertiary bg-tertiary-container';
    default:
      return 'text-on-surface-variant bg-surface-container-high';
  }
}
