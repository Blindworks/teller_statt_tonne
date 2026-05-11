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
  Partner,
  STATUS_LABELS,
  WEEKDAYS,
  Weekday,
} from '../partners/partner.model';
import { PartnerService } from '../partners/partner.service';
import { PartnerCategoryRegistry } from '../partners/partner-category-registry.service';
import { AuthService } from '../auth/auth.service';
import { StoreDetailDialogService } from '../stores/store-detail-dialog/store-detail-dialog.service';
import {
  buildDistributionPointMarkerIcon,
  buildEventMarkerIcon,
  buildPartnerMarkerIcon,
} from './map-marker-icon';
import { DistributionPointService } from '../admin/distribution-points/distribution-point.service';
import {
  DistributionPoint,
  WEEKDAY_LABELS,
} from '../admin/distribution-points/distribution-point.model';
import { EventService } from '../events/event.service';
import { CharityEvent } from '../events/event.model';

type DayFilter = 'ALL' | Weekday;

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
  private readonly distributionPointService = inject(DistributionPointService);
  private readonly eventService = inject(EventService);
  private readonly auth = inject(AuthService);
  private readonly detailDialog = inject(StoreDetailDialogService);
  private readonly categoryRegistry = inject(PartnerCategoryRegistry);

  @ViewChild('mapContainer', { static: true })
  private mapContainerRef!: ElementRef<HTMLDivElement>;

  readonly partners = signal<Partner[]>([]);
  readonly distributionPoints = signal<DistributionPoint[]>([]);
  readonly events = signal<CharityEvent[]>([]);
  readonly loadError = signal<string | null>(null);

  readonly selectedCategories = signal<Set<number>>(new Set());
  private categoryInit = false;
  readonly activeOnly = signal(false);
  readonly selectedDay = signal<DayFilter>('ALL');
  readonly showDistributionPoints = signal(true);
  readonly showEvents = signal(true);

  readonly weekdays = WEEKDAYS;
  readonly allCategories = this.categoryRegistry.categories;

  categoryIcon(id: number | null): string {
    return this.categoryRegistry.iconForId(id);
  }
  categoryLabel(id: number | null): string {
    return this.categoryRegistry.labelForId(id);
  }

  readonly filteredPartners = computed(() => {
    const cats = this.selectedCategories();
    const onlyActive = this.activeOnly();
    const day = this.selectedDay();
    return this.partners().filter((p) => {
      if (p.categoryId == null || !cats.has(p.categoryId)) return false;
      if (onlyActive && p.status !== 'KOOPERIERT') return false;
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

  readonly filteredDistributionPoints = computed(() => {
    if (!this.showDistributionPoints()) return [];
    return this.distributionPoints().filter((dp) => dp.latitude != null && dp.longitude != null);
  });

  readonly filteredEvents = computed(() => {
    if (!this.showEvents()) return [];
    return this.events().filter((ev) => ev.latitude != null && ev.longitude != null);
  });

  private map?: L.Map;
  private cluster?: L.MarkerClusterGroup;
  private routeLayer?: L.Polyline;

  constructor() {
    this.service.list().subscribe({
      next: (list) => this.partners.set(list),
      error: () => this.loadError.set('Betriebe konnten nicht geladen werden.'),
    });

    const roles = this.auth.currentUser()?.roles ?? [];
    if (roles.includes('ADMINISTRATOR') || roles.includes('TEAMLEITER')) {
      this.distributionPointService.list().subscribe({
        next: (list) => this.distributionPoints.set(list),
        error: () => {
          // Verteilerplätze sind optional auf der Karte — Fehler still ignorieren
        },
      });
    }

    this.eventService.list('active').subscribe({
      next: (list) => this.events.set(list),
      error: () => {
        // Veranstaltungen sind optional auf der Karte — Fehler still ignorieren
      },
    });

    effect(() => {
      const cats = this.allCategories();
      if (!this.categoryInit && cats.length > 0) {
        this.categoryInit = true;
        this.selectedCategories.set(
          new Set(cats.map((c) => c.id).filter((id): id is number => id != null)),
        );
      }
    });

    effect(() => {
      this.filteredPartners();
      this.filteredDistributionPoints();
      this.filteredEvents();
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

  toggleCategory(id: number): void {
    this.selectedCategories.update((s) => {
      const next = new Set(s);
      if (next.has(id)) {
        next.delete(id);
      } else {
        next.add(id);
      }
      return next;
    });
  }

  isCategorySelected(id: number): boolean {
    return this.selectedCategories().has(id);
  }

  toggleActiveOnly(): void {
    this.activeOnly.update((v) => !v);
  }

  setDay(day: DayFilter): void {
    this.selectedDay.set(day);
  }

  toggleDistributionPoints(): void {
    this.showDistributionPoints.update((v) => !v);
  }

  toggleEvents(): void {
    this.showEvents.update((v) => !v);
  }

  canSeeDistributionPoints(): boolean {
    const roles = this.auth.currentUser()?.roles ?? [];
    return roles.includes('ADMINISTRATOR') || roles.includes('TEAMLEITER');
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
      marker.on('popupopen', (e: L.PopupEvent) => {
        const el = e.popup.getElement()?.querySelector<HTMLElement>('[data-detail-id]');
        if (!el) return;
        el.onclick = (ev) => {
          ev.preventDefault();
          this.detailDialog.open(p);
          marker.closePopup();
        };
      });
      markers.push(marker);
    });
    markers.forEach((m) => this.cluster!.addLayer(m));

    const dpMarkers: L.Marker[] = [];
    for (const dp of this.filteredDistributionPoints()) {
      const marker = L.marker([dp.latitude!, dp.longitude!], {
        icon: buildDistributionPointMarkerIcon(),
      });
      marker.bindPopup(this.buildDistributionPointPopup(dp));
      dpMarkers.push(marker);
    }
    dpMarkers.forEach((m) => this.cluster!.addLayer(m));

    const eventMarkers: L.Marker[] = [];
    for (const ev of this.filteredEvents()) {
      const marker = L.marker([ev.latitude!, ev.longitude!], {
        icon: buildEventMarkerIcon(),
      });
      marker.bindPopup(this.buildEventPopup(ev));
      eventMarkers.push(marker);
    }
    eventMarkers.forEach((m) => this.cluster!.addLayer(m));

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

    const allPoints: L.LatLngTuple[] = [
      ...ordered.map((p) => [p.latitude!, p.longitude!] as L.LatLngTuple),
      ...this.filteredDistributionPoints().map(
        (dp) => [dp.latitude!, dp.longitude!] as L.LatLngTuple,
      ),
      ...this.filteredEvents().map((ev) => [ev.latitude!, ev.longitude!] as L.LatLngTuple),
    ];
    if (allPoints.length > 0) {
      const bounds = L.latLngBounds(allPoints);
      this.map.fitBounds(bounds, { padding: [40, 40], maxZoom: 15 });
    } else {
      this.map.setView(DEFAULT_CENTER, DEFAULT_ZOOM);
    }
  }

  private buildDistributionPointPopup(dp: DistributionPoint): string {
    const escape = (s: string) =>
      s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
    const addressLine = [dp.street, [dp.postalCode, dp.city].filter((p) => !!p).join(' ').trim()]
      .filter((p) => !!p && p.trim())
      .map((p) => escape(p!.trim()))
      .join(', ');
    const hours = dp.openingSlots.length
      ? `<ul class="map-popup__hours">${dp.openingSlots
          .map(
            (s) =>
              `<li>${escape(WEEKDAY_LABELS[s.weekday])} · ${escape(s.startTime)}–${escape(s.endTime)}</li>`,
          )
          .join('')}</ul>`
      : '';
    const isPlanner = this.canSeeDistributionPoints();
    const editLink = isPlanner
      ? `<a class="map-popup__link" href="/admin/distribution-points/${dp.id}">Bearbeiten →</a>`
      : '';
    return `
      <div class="map-popup">
        <div class="map-popup__title">${escape(dp.name)} <span class="map-popup__badge map-popup__badge--active">Verteilerplatz</span></div>
        <div class="map-popup__category">Teller-Treff</div>
        ${addressLine ? `<div class="map-popup__address">${addressLine}</div>` : ''}
        ${hours}
        ${editLink}
      </div>
    `;
  }

  private buildEventPopup(ev: CharityEvent): string {
    const escape = (s: string) =>
      s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
    const addressLine = [ev.street, [ev.postalCode, ev.city].filter((p) => !!p).join(' ').trim()]
      .filter((p) => !!p && p.trim())
      .map((p) => escape(p!.trim()))
      .join(', ');
    const period =
      ev.startDate === ev.endDate
        ? escape(ev.startDate)
        : `${escape(ev.startDate)} – ${escape(ev.endDate)}`;
    const isPlanner = this.canSeeDistributionPoints();
    const editLink = isPlanner
      ? `<a class="map-popup__link" href="/events/${ev.id}">Bearbeiten →</a>`
      : '';
    return `
      <div class="map-popup">
        <div class="map-popup__title">${escape(ev.name)} <span class="map-popup__badge map-popup__badge--active">Veranstaltung</span></div>
        <div class="map-popup__category">${period}</div>
        ${addressLine ? `<div class="map-popup__address">${addressLine}</div>` : ''}
        ${editLink}
      </div>
    `;
  }

  private buildIcon(partner: Partner, order: number | null): L.DivIcon {
    return buildPartnerMarkerIcon(partner, this.categoryRegistry.byId(partner.categoryId), order);
  }

  private buildPopup(p: Partner): string {
    const statusBadge =
      p.status === 'KOOPERIERT'
        ? `<span class="map-popup__badge map-popup__badge--active">${STATUS_LABELS[p.status]}</span>`
        : `<span class="map-popup__badge">${STATUS_LABELS[p.status]}</span>`;
    const editHref = `/stores/edit/${p.id}`;
    const isRetter = !!this.auth.currentUser()?.roles?.includes('RETTER');
    const actionLink = isRetter
      ? `<a class="map-popup__link" href="#" data-detail-id="${p.id}">Details ansehen →</a>`
      : `<a class="map-popup__link" href="${editHref}">Bearbeiten →</a>`;
    const escape = (s: string) =>
      s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
    return `
      <div class="map-popup">
        <div class="map-popup__title">${escape(p.name)} ${statusBadge}</div>
        <div class="map-popup__category">${escape(this.categoryRegistry.labelForId(p.categoryId))}</div>
        <div class="map-popup__address">${escape(p.street ?? '')}, ${escape(p.postalCode ?? '')} ${escape(p.city ?? '')}</div>
        ${actionLink}
      </div>
    `;
  }
}
