import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { Router } from '@angular/router';
import { CATEGORY_ICONS, CATEGORY_LABELS, Category } from '../partners/partner.model';
import { PickupAssignment } from '../pickups/pickup.model';
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
  private readonly router = inject(Router);

  private readonly daySlots = toSignal(this.dashboardService.day(), {
    initialValue: [] as DaySlot[],
  });

  readonly displaySlots = computed<DisplaySlot[]>(() =>
    this.daySlots().map((s, idx) => this.toDisplay(s, idx)),
  );

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
        memberName: a?.memberName ?? null,
        avatarUrl: a?.memberAvatarUrl ?? null,
        initial: this.initial(a?.memberName ?? null),
      });
    }

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
      freeCount: Math.max(0, capacity - assignments.length),
      chips,
    };
  }

  private initial(name: string | null): string {
    if (!name) return '?';
    const trimmed = name.trim();
    return trimmed.length > 0 ? trimmed.charAt(0).toUpperCase() : '?';
  }
}
