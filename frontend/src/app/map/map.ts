import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  OnDestroy,
  ViewChild,
  computed,
  effect,
  inject,
  signal,
} from '@angular/core';
import { L } from './leaflet-global';
import 'leaflet.markercluster';
import {
  CATEGORY_ICONS,
  CATEGORY_LABELS,
  Category,
  Partner,
  WEEKDAYS,
  Weekday,
} from '../partners/partner.model';
import { PartnerService } from '../partners/partner.service';
import { AuthService } from '../auth/auth.service';
import { buildPartnerMarkerIcon } from './map-marker-icon';

type DayFilter = 'ALL' | Weekday;

const ALL_CATEGORIES: Category[] = ['BAKERY', 'SUPERMARKET', 'CAFE', 'RESTAURANT'];

const DEFAULT_CENTER: L.LatLngTuple = [50.1817, 8.74];
const DEFAULT_ZOOM = 13;

@Component({
  selector: 'app-map',
  standalone: true,
  templateUrl: './map.html',
  styleUrl: './map.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MapComponent implements AfterViewInit, OnDestroy {
  private readonly service = inject(PartnerService);
  private readonly auth = inject(AuthService);

  @ViewChild('mapContainer', { static: true })
  private mapContainerRef!: ElementRef<HTMLDivElement>;

  readonly partners = signal<Partner[]>([]);
  readonly loadError = signal<string | null>(null);

  readonly selectedCategories = signal<Set<Category>>(new Set(ALL_CATEGORIES));
  readonly activeOnly = signal(false);
  readonly selectedDay = signal<DayFilter>('ALL');

  readonly categoryLabels = CATEGORY_LABELS;
  readonly categoryIcons = CATEGORY_ICONS;
  readonly weekdays = WEEKDAYS;
  readonly allCategories = ALL_CATEGORIES;

  readonly filteredPartners = computed(() => {
    const cats = this.selectedCategories();
    const onlyActive = this.activeOnly();
    const day = this.selectedDay();
    return this.partners().filter((p) => {
      if (!cats.has(p.category)) return false;
      if (onlyActive && p.status !== 'ACTIVE') return false;
      if (day !== 'ALL') {
        const slot = p.pickupSlots.find((s) => s.weekday === day && s.active);
        if (!slot) return false;
      }
      return p.latitude != null && p.longitude != null;
    });
  });

  readonly missingCoordsCount = computed(
    () => this.partners().filter((p) => p.latitude == null || p.longitude == null).length,
  );

  private map?: L.Map;
  private cluster?: L.MarkerClusterGroup;
  private routeLayer?: L.Polyline;

  constructor() {
    this.service.list().subscribe({
      next: (list) => this.partners.set(list),
      error: () => this.loadError.set('Partner konnten nicht geladen werden.'),
    });

    effect(() => {
      this.filteredPartners();
      this.selectedDay();
      if (this.map) {
        this.redraw();
      }
    });
  }

  ngAfterViewInit(): void {
    this.map = L.map(this.mapContainerRef.nativeElement, {
      center: DEFAULT_CENTER,
      zoom: DEFAULT_ZOOM,
    });
    L.tileLayer('https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png', {
      attribution: '&copy; OpenStreetMap &copy; CARTO',
      subdomains: 'abcd',
      maxZoom: 19,
    }).addTo(this.map);
    this.cluster = L.markerClusterGroup();
    this.map.addLayer(this.cluster);
    this.redraw();
  }

  ngOnDestroy(): void {
    this.map?.remove();
    this.map = undefined;
  }

  toggleCategory(cat: Category): void {
    this.selectedCategories.update((s) => {
      const next = new Set(s);
      if (next.has(cat)) {
        next.delete(cat);
      } else {
        next.add(cat);
      }
      return next;
    });
  }

  isCategorySelected(cat: Category): boolean {
    return this.selectedCategories().has(cat);
  }

  toggleActiveOnly(): void {
    this.activeOnly.update((v) => !v);
  }

  setDay(day: DayFilter): void {
    this.selectedDay.set(day);
  }

  private redraw(): void {
    if (!this.map || !this.cluster) return;

    this.cluster.clearLayers();
    if (this.routeLayer) {
      this.map.removeLayer(this.routeLayer);
      this.routeLayer = undefined;
    }

    const partners = this.filteredPartners();
    const day = this.selectedDay();

    const ordered =
      day === 'ALL'
        ? partners
        : [...partners].sort((a, b) => {
            const sa = a.pickupSlots.find((s) => s.weekday === day && s.active)?.startTime ?? '';
            const sb = b.pickupSlots.find((s) => s.weekday === day && s.active)?.startTime ?? '';
            return sa.localeCompare(sb);
          });

    const markers: L.Marker[] = [];
    ordered.forEach((p, idx) => {
      const marker = L.marker([p.latitude!, p.longitude!], {
        icon: this.buildIcon(p, day === 'ALL' ? null : idx + 1),
      });
      marker.bindPopup(this.buildPopup(p));
      markers.push(marker);
    });
    markers.forEach((m) => this.cluster!.addLayer(m));

    if (day !== 'ALL' && ordered.length >= 2) {
      const latlngs: L.LatLngTuple[] = ordered.map((p) => [p.latitude!, p.longitude!]);
      this.routeLayer = L.polyline(latlngs, {
        color: '#116e20',
        weight: 4,
        opacity: 0.75,
        dashArray: '4 10',
        lineCap: 'round',
      }).addTo(this.map);
    }

    if (markers.length > 0) {
      const bounds = L.latLngBounds(ordered.map((p) => [p.latitude!, p.longitude!] as L.LatLngTuple));
      this.map.fitBounds(bounds, { padding: [40, 40], maxZoom: 15 });
    } else {
      this.map.setView(DEFAULT_CENTER, DEFAULT_ZOOM);
    }
  }

  private buildIcon(partner: Partner, order: number | null): L.DivIcon {
    return buildPartnerMarkerIcon(partner, order);
  }

  private buildPopup(p: Partner): string {
    const statusBadge =
      p.status === 'ACTIVE'
        ? '<span class="map-popup__badge map-popup__badge--active">Aktiv</span>'
        : '<span class="map-popup__badge">Inaktiv</span>';
    const editHref = `/stores/edit/${p.id}`;
    const isRetter = !!this.auth.currentUser()?.roles?.includes('RETTER');
    const editLink = isRetter
      ? ''
      : `<a class="map-popup__link" href="${editHref}">Bearbeiten →</a>`;
    const escape = (s: string) =>
      s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
    return `
      <div class="map-popup">
        <div class="map-popup__title">${escape(p.name)} ${statusBadge}</div>
        <div class="map-popup__category">${escape(CATEGORY_LABELS[p.category])}</div>
        <div class="map-popup__address">${escape(p.street ?? '')}, ${escape(p.postalCode ?? '')} ${escape(p.city ?? '')}</div>
        ${editLink}
      </div>
    `;
  }
}
