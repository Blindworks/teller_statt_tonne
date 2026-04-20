import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { CATEGORY_ICONS, CATEGORY_LABELS, Category } from '../partners/partner.model';
import { Pickup } from '../pickups/pickup.model';
import { PickupService } from '../pickups/pickup.service';

interface NewsItem {
  type: 'event' | 'milestone' | 'maintenance';
  timeAgo: string;
  title: string;
  description: string;
}

interface DisplayPickup {
  id: string;
  store: string;
  image: string | null;
  location: string;
  time: string;
  badgeLabel: string;
  isExpiring: boolean;
  categoryIcon: string;
}

@Component({
  selector: 'app-dashboard',
  imports: [],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DashboardComponent {
  private readonly pickupService = inject(PickupService);

  private readonly upcomingPickups = toSignal(this.pickupService.upcoming(3), {
    initialValue: [] as Pickup[],
  });

  private readonly now = signal(new Date());

  readonly displayPickups = computed<DisplayPickup[]>(() =>
    this.upcomingPickups().map((p) => this.toDisplay(p, this.now())),
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

  private toDisplay(p: Pickup, now: Date): DisplayPickup {
    const category: Category | null = p.partnerCategory;
    const categoryIcon = category ? CATEGORY_ICONS[category] : 'storefront';
    const categoryLabel = category ? CATEGORY_LABELS[category] : 'Pickup';

    const location = [p.partnerStreet, p.partnerCity].filter((s) => !!s).join(', ');

    const isExpiring = this.isStartingSoon(p, now);

    return {
      id: p.id ?? '',
      store: p.partnerName ?? 'Unbekannter Partner',
      image: p.partnerLogoUrl,
      location: location || '—',
      time: this.formatTime(p, now),
      badgeLabel: isExpiring ? 'Expiring Soon' : categoryLabel,
      isExpiring,
      categoryIcon,
    };
  }

  private isStartingSoon(p: Pickup, now: Date): boolean {
    const start = this.parseDateTime(p.date, p.startTime);
    if (!start) return false;
    const diffMs = start.getTime() - now.getTime();
    const twoHoursMs = 2 * 60 * 60 * 1000;
    return diffMs >= 0 && diffMs <= twoHoursMs;
  }

  private formatTime(p: Pickup, now: Date): string {
    const today = this.toDateKey(now);
    const tomorrow = this.toDateKey(new Date(now.getTime() + 24 * 60 * 60 * 1000));
    if (p.date === today) return `${p.startTime} Heute`;
    if (p.date === tomorrow) return `${p.startTime} Morgen`;
    const [y, m, d] = p.date.split('-');
    return `${p.startTime}, ${d}.${m}.${y}`;
  }

  private toDateKey(d: Date): string {
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${y}-${m}-${day}`;
  }

  private parseDateTime(date: string, time: string): Date | null {
    if (!date || !time) return null;
    const [y, mo, d] = date.split('-').map(Number);
    const [h, mi] = time.split(':').map(Number);
    if ([y, mo, d, h, mi].some((n) => Number.isNaN(n))) return null;
    return new Date(y, mo - 1, d, h, mi);
  }
}
