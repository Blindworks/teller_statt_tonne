import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  HostListener,
  OnDestroy,
  ViewChild,
  computed,
  effect,
  inject,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { L } from '../../map/leaflet-global';
import {
  CATEGORY_ICONS,
  CATEGORY_LABELS,
  PickupSlot,
  WEEKDAYS,
  Weekday,
} from '../../partners/partner.model';
import { AuthService } from '../../auth/auth.service';
import { StoreDetailDialogService } from './store-detail-dialog.service';

const WEEKDAY_LABELS: Record<Weekday, string> = WEEKDAYS.reduce(
  (acc, w) => {
    acc[w.value] = w.label;
    return acc;
  },
  {} as Record<Weekday, string>,
);

const WEEKDAY_ORDER: Record<Weekday, number> = WEEKDAYS.reduce(
  (acc, w, idx) => {
    acc[w.value] = idx;
    return acc;
  },
  {} as Record<Weekday, number>,
);

@Component({
  selector: 'app-store-detail-dialog',
  imports: [RouterLink],
  templateUrl: './store-detail-dialog.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StoreDetailDialogComponent implements AfterViewInit, OnDestroy {
  private readonly dialogService = inject(StoreDetailDialogService);
  private readonly auth = inject(AuthService);

  readonly partner = this.dialogService.partner;
  readonly isRetter = computed(() => !!this.auth.currentUser()?.roles?.includes('RETTER'));
  readonly categoryLabels = CATEGORY_LABELS;
  readonly categoryIcons = CATEGORY_ICONS;

  readonly activeSlots = computed<PickupSlot[]>(() => {
    const p = this.partner();
    if (!p) return [];
    return [...p.pickupSlots]
      .filter((s) => s.active)
      .sort(this.compareSlots);
  });

  readonly inactiveSlots = computed<PickupSlot[]>(() => {
    const p = this.partner();
    if (!p) return [];
    return [...p.pickupSlots]
      .filter((s) => !s.active)
      .sort(this.compareSlots);
  });

  readonly hasCoords = computed(() => {
    const p = this.partner();
    return !!p && p.latitude != null && p.longitude != null;
  });

  @ViewChild('mapContainer')
  private mapContainerRef?: ElementRef<HTMLDivElement>;

  private map?: L.Map;
  private marker?: L.Marker;

  constructor() {
    effect(() => {
      const p = this.partner();
      if (!p) {
        this.destroyMap();
        return;
      }
      queueMicrotask(() => this.syncMap());
    });
  }

  ngAfterViewInit(): void {
    this.syncMap();
  }

  ngOnDestroy(): void {
    this.destroyMap();
  }

  weekdayLabel(weekday: Weekday): string {
    return WEEKDAY_LABELS[weekday];
  }

  close(): void {
    this.dialogService.close();
  }

  onBackdrop(event: MouseEvent): void {
    if (event.target === event.currentTarget) {
      this.close();
    }
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (this.partner() != null) this.close();
  }

  private compareSlots = (a: PickupSlot, b: PickupSlot): number => {
    const wd = WEEKDAY_ORDER[a.weekday] - WEEKDAY_ORDER[b.weekday];
    if (wd !== 0) return wd;
    return a.startTime.localeCompare(b.startTime);
  };

  private syncMap(): void {
    const p = this.partner();
    const container = this.mapContainerRef?.nativeElement;
    if (!container || !p || p.latitude == null || p.longitude == null) {
      this.destroyMap();
      return;
    }

    const coords: L.LatLngTuple = [p.latitude, p.longitude];

    if (!this.map) {
      this.map = L.map(container, {
        center: coords,
        zoom: 15,
        zoomControl: false,
        attributionControl: false,
        dragging: false,
        scrollWheelZoom: false,
        doubleClickZoom: false,
        boxZoom: false,
        keyboard: false,
        touchZoom: false,
      });
      L.tileLayer('https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png', {
        subdomains: 'abcd',
        maxZoom: 19,
      }).addTo(this.map);
    } else {
      this.map.setView(coords, 15);
    }

    if (this.marker) {
      this.marker.setLatLng(coords);
    } else {
      this.marker = L.marker(coords).addTo(this.map);
    }

    setTimeout(() => this.map?.invalidateSize(), 0);
  }

  private destroyMap(): void {
    this.marker = undefined;
    this.map?.remove();
    this.map = undefined;
  }
}
