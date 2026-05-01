import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  EventEmitter,
  Input,
  OnDestroy,
  Output,
  ViewChild,
  inject,
  signal,
} from '@angular/core';
import { DecimalPipe } from '@angular/common';
import * as L from 'leaflet';
import { PartnerService, ReverseGeocodeResult } from '../partner.service';

const DEFAULT_CENTER: L.LatLngTuple = [50.1817, 8.74];
const DEFAULT_ZOOM = 14;

const PICKER_ICON: L.DivIcon = L.divIcon({
  className: 'picker-marker',
  html: `
    <div class="picker-marker__pin">
      <span class="material-symbols-outlined">place</span>
    </div>
    <svg class="picker-marker__tail" viewBox="0 0 12 10" aria-hidden="true">
      <path d="M0 0 H12 L6 10 Z"/>
    </svg>
  `,
  iconSize: [36, 44],
  iconAnchor: [18, 42],
});

export interface LocationPickResult {
  latitude: number;
  longitude: number;
  street: string | null;
  postalCode: string | null;
  city: string | null;
}

@Component({
  selector: 'app-location-picker-dialog',
  standalone: true,
  imports: [DecimalPipe],
  templateUrl: './location-picker-dialog.html',
  styleUrl: './location-picker-dialog.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LocationPickerDialogComponent implements AfterViewInit, OnDestroy {
  private readonly service = inject(PartnerService);

  @Input() initialLatitude: number | null = null;
  @Input() initialLongitude: number | null = null;

  @Output() readonly accepted = new EventEmitter<LocationPickResult>();
  @Output() readonly cancelled = new EventEmitter<void>();

  @ViewChild('mapContainer', { static: true })
  private mapContainerRef!: ElementRef<HTMLDivElement>;

  readonly pickedLat = signal<number | null>(null);
  readonly pickedLon = signal<number | null>(null);
  readonly resolvedAddress = signal<ReverseGeocodeResult | null>(null);
  readonly resolving = signal(false);
  readonly resolveMessage = signal<string | null>(null);

  private map?: L.Map;
  private marker?: L.Marker;

  ngAfterViewInit(): void {
    const start: L.LatLngTuple =
      this.initialLatitude != null && this.initialLongitude != null
        ? [this.initialLatitude, this.initialLongitude]
        : DEFAULT_CENTER;
    const startZoom = this.initialLatitude != null ? 15 : DEFAULT_ZOOM;

    this.map = L.map(this.mapContainerRef.nativeElement, {
      center: start,
      zoom: startZoom,
    });
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '&copy; OpenStreetMap-Mitwirkende',
      maxZoom: 19,
    }).addTo(this.map);

    if (this.initialLatitude != null && this.initialLongitude != null) {
      this.placePin(this.initialLatitude, this.initialLongitude, false);
    }

    this.map.on('click', (event: L.LeafletMouseEvent) => {
      this.placePin(event.latlng.lat, event.latlng.lng, true);
    });
  }

  ngOnDestroy(): void {
    this.map?.remove();
    this.map = undefined;
  }

  cancel(): void {
    this.cancelled.emit();
  }

  accept(): void {
    const lat = this.pickedLat();
    const lon = this.pickedLon();
    if (lat == null || lon == null) return;
    const addr = this.resolvedAddress();
    this.accepted.emit({
      latitude: lat,
      longitude: lon,
      street: addr?.street ?? null,
      postalCode: addr?.postalCode ?? null,
      city: addr?.city ?? null,
    });
  }

  private placePin(lat: number, lon: number, lookup: boolean): void {
    if (!this.map) return;
    this.pickedLat.set(lat);
    this.pickedLon.set(lon);

    if (this.marker) {
      this.marker.setLatLng([lat, lon]);
    } else {
      this.marker = L.marker([lat, lon], { draggable: true, icon: PICKER_ICON }).addTo(this.map);
      this.marker.on('dragend', () => {
        const pos = this.marker!.getLatLng();
        this.placePin(pos.lat, pos.lng, true);
      });
    }
    if (lookup) {
      this.lookupAddress(lat, lon);
    }
  }

  private lookupAddress(lat: number, lon: number): void {
    this.resolving.set(true);
    this.resolveMessage.set(null);
    this.resolvedAddress.set(null);
    this.service.reverseGeocode(lat, lon).subscribe({
      next: (result) => {
        this.resolving.set(false);
        if (!result) {
          this.resolveMessage.set('Keine Adresse gefunden — Pin wird trotzdem übernommen.');
          return;
        }
        this.resolvedAddress.set(result);
      },
      error: () => {
        this.resolving.set(false);
        this.resolveMessage.set('Adresssuche fehlgeschlagen — Pin wird trotzdem übernommen.');
      },
    });
  }
}
