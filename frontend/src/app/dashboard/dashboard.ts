import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../auth/auth.service';
import { CATEGORY_ICONS, CATEGORY_LABELS, Category } from '../partners/partner.model';
import { PickupAssignment } from '../pickups/pickup.model';
import { resolvePhotoUrl } from '../users/photo-url';
import { Role } from '../users/user.model';
import { UserProfileDialogService } from '../users/user-profile-dialog/user-profile-dialog.service';
import { DaySlot } from './day-slot.model';
import { DashboardService } from './dashboard.service';

interface NewsItem {
  type: 'event' | 'milestone' | 'maintenance';
  timeAgo: string;
  title: string;
  description: string;
}

interface SlotChip {
  filled: boolean;
  memberId: number | null;
  memberName: string | null;
  avatarUrl: string | null;
  initial: string;
}

interface DisplaySlot {
  key: string;
  pickupId: number | null;
  partnerId: number;
  partnerName: string;
  location: string;
  time: string;
  date: string;
  startTime: string;
  endTime: string;
  badgeLabel: string;
  isTemplate: boolean;
  categoryIcon: string;
  logoUrl: string | null;
  capacity: number;
  assignedCount: number;
  freeCount: number;
  chips: SlotChip[];
  currentUserAssigned: boolean;
  canSignup: boolean;
}

@Component({
  selector: 'app-dashboard',
  imports: [],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DashboardComponent {
  private readonly dashboardService = inject(DashboardService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly profileDialog = inject(UserProfileDialogService);

  private readonly daySlotsSignal = signal<DaySlot[]>([]);
  private readonly errorSignal = signal<string | null>(null);

  readonly displaySlots = computed<DisplaySlot[]>(() =>
    this.daySlotsSignal().map((s, idx) => this.toDisplay(s, idx)),
  );

  readonly userRole = computed<Role | null>(() => this.authService.currentUser()?.role ?? null);
  readonly firstName = computed<string>(() => this.authService.currentUser()?.firstName ?? '');
  readonly isRetter = computed(() => this.userRole() === 'RETTER');
  readonly isAdminOrAmbassador = computed(() => {
    const role = this.userRole();
    return role === 'ADMINISTRATOR' || role === 'BOTSCHAFTER';
  });
  readonly isNewMember = computed(() => this.userRole() === 'NEW_MEMBER');
  readonly errorMessage = this.errorSignal.asReadonly();

  constructor() {
    this.loadSlots();
  }

  readonly news: NewsItem[] = [
    {
      type: 'event',
      timeAgo: '2h ago',
      title: 'Summer Potluck at the Park next Sunday!',
      description: 'Bring your saved recipes and join us for a sunset feast at Park West.',
    },
    {
      type: 'milestone',
      timeAgo: '5h ago',
      title: "New Store: 'Veggie Bliss' joins the network",
      description: 'Our first dedicated vegan store is now available for pickups starting Monday.',
    },
    {
      type: 'maintenance',
      timeAgo: '1d ago',
      title: 'Updated Pickup Guidelines',
      description: 'Please read the new hygiene protocols for fresh produce handling.',
    },
  ];

  readonly activeAvatars = [
    'https://lh3.googleusercontent.com/aida-public/AB6AXuBj6LwGbCnoAgLSby3-xuN-scl2xdK_DTFQt9q8Drz_z8prWDKyT1JXVSxEqyyyzJDpxiW9JkNKltRKp3eGiXpq_WIoN2HHDH_9iOgxdm3BhLm3jP67pv0YnW2ymTptqItwj3xEByWSykcXg4InUHfIfxr38XTmy32GjifNC0xbPzUerRNqIFWof173glFhogMofDKcO9eXfeypu82yrJ63edO4d3oKWMJm8HyHOuDj6-bCbJGQIszfUQIektOC1ZQEu42COdRVwRwG',
    'https://lh3.googleusercontent.com/aida-public/AB6AXuC_3J34nPEsvIbRVjvTvSDxgTa2IcrdzEjS7tNnTxPJDD5Scxn_dUaftO_gRjQ-hossMhGBgvp4nox5d7imb1SRLHWudLrr_MDljL6RAy9lvGp_k-naMIlv2QuRWJM_OZ9Dpq4ziMESnHfB7637e7hfARifhKh4Y9FDdv_LKCaCgy6smBckJLyog9N3bN4I15Y2DyS44f47RvJ33flmk-BEfZ97ctsVjfpz62RTbr-S5l5Dyq5mPyx7reYmJK8x-4MtzuTKFRmaXhRJ',
    'https://lh3.googleusercontent.com/aida-public/AB6AXuBZJaxHkkwsCXL9rBAbuiAcW4iWoQ114YgVUxRN4drIebkX-JSEhchHq1EskpN75EeV1bRWwyYGiKYTsz8ZRRVCqte7AXnuGaax7qZipzTiOmjDIZ40_gUsP3rRm5Pgc6Zgo5XJemn7fCHdr90wl91CLhamT0XbEcugTFsR6Hav8gCRVt81omhT7zkqbWdP86ns3gmJ1yUa447BEEKU83IkTMUNA_1LeZ2NFVLamHOpZ8jii4Toq0xzEzOdnY5FPMTugrji8ZyF_OjV',
  ];

  readonly profileImage =
    'https://lh3.googleusercontent.com/aida-public/AB6AXuAlnI7Jvlz_ItVX5RMs8c1rQ2KnMKp8akokDrB8ge2wAaZPKWb0ZDKUztGT9bQkumnREcvOTokVb7yTetcJwvZJIctkI5SOdI3iYH6EcWu-6h6KRX4XNypVJaFdgZglXJMWHELSUGH_u0Lvqx7Yy0AEwqDJ5KHcNMqF8eTxPtwdDcRpjpv75EulDc28zDPv0eIEFRMS9w0I8Yw0rbYWBF4HU9IFqaNxK5ICJ83u0UqpC-UhY4-fq1vwPvtT02JkgJhBJhKB8gWkHaQu';

  openProfile(memberId: number | null): void {
    if (memberId == null) return;
    this.profileDialog.open(memberId);
  }

  openSlot(slot: DisplaySlot): void {
    if (slot.pickupId != null) {
      this.router.navigate(['/pickups/edit', slot.pickupId]);
      return;
    }
    this.router.navigate(['/pickups/new'], {
      queryParams: {
        partnerId: slot.partnerId,
        date: slot.date,
        startTime: slot.startTime,
        endTime: slot.endTime,
      },
    });
  }

  signup(slot: DisplaySlot): void {
    if (slot.pickupId == null) return;
    this.errorSignal.set(null);
    this.dashboardService.signup(slot.pickupId).subscribe({
      next: () => this.loadSlots(),
      error: (err) => this.errorSignal.set(this.errorText(err, 'Eintragen fehlgeschlagen.')),
    });
  }

  unassign(slot: DisplaySlot): void {
    if (slot.pickupId == null) return;
    this.errorSignal.set(null);
    this.dashboardService.unassign(slot.pickupId).subscribe({
      next: () => this.loadSlots(),
      error: (err) => this.errorSignal.set(this.errorText(err, 'Austragen fehlgeschlagen.')),
    });
  }

  private loadSlots(): void {
    this.dashboardService.day().subscribe({
      next: (slots) => this.daySlotsSignal.set(slots),
      error: () => this.daySlotsSignal.set([]),
    });
  }

  private errorText(err: unknown, fallback: string): string {
    const status = (err as { status?: number })?.status;
    if (status === 403) return 'Du bist diesem Store nicht zugeordnet.';
    if (status === 409) return 'Slot ist bereits voll.';
    if (status === 404) return 'Slot wurde nicht gefunden.';
    return fallback;
  }

  private toDisplay(s: DaySlot, idx: number): DisplaySlot {
    const category: Category | null = s.partnerCategory;
    const categoryIcon = category ? CATEGORY_ICONS[category] : 'storefront';
    const categoryLabel = category ? CATEGORY_LABELS[category] : 'Pickup';

    const location = [s.partnerStreet, s.partnerCity].filter((x) => !!x).join(', ');
    const assignments = s.assignments ?? [];
    const capacity = Math.max(s.capacity, assignments.length);
    const chips: SlotChip[] = [];
    for (let i = 0; i < capacity; i++) {
      const a: PickupAssignment | undefined = assignments[i];
      chips.push({
        filled: !!a,
        memberId: a?.memberId ?? null,
        memberName: a?.memberName ?? null,
        avatarUrl: resolvePhotoUrl(a?.memberAvatarUrl ?? null),
        initial: this.initial(a?.memberName ?? null),
      });
    }

    const freeCount = Math.max(0, capacity - assignments.length);

    return {
      key: `${s.pickupId ?? 't'}-${s.partnerId}-${s.startTime}-${idx}`,
      pickupId: s.pickupId,
      partnerId: s.partnerId,
      partnerName: s.partnerName,
      location: location || '—',
      time: `${s.startTime} – ${s.endTime}`,
      date: s.date,
      startTime: s.startTime,
      endTime: s.endTime,
      badgeLabel: s.isTemplate ? 'Slot frei' : categoryLabel,
      isTemplate: s.isTemplate,
      categoryIcon,
      logoUrl: s.partnerLogoUrl,
      capacity,
      assignedCount: assignments.length,
      freeCount,
      chips,
      currentUserAssigned: s.currentUserAssigned,
      canSignup: !s.isTemplate && !s.currentUserAssigned && freeCount > 0,
    };
  }

  private initial(name: string | null): string {
    if (!name) return '?';
    const trimmed = name.trim();
    return trimmed.length > 0 ? trimmed.charAt(0).toUpperCase() : '?';
  }
}
