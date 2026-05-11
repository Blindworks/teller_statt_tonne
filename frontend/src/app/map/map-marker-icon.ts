import { L } from './leaflet-global';
import { Status } from '../partners/partner.model';
import { PartnerCategory } from '../admin/partner-categories/partner-category.model';

export function buildPartnerMarkerIcon(
  partner: { categoryId: number | null; status: Status },
  category: PartnerCategory | null,
  order: number | null = null,
): L.DivIcon {
  const symbol = category?.icon ?? 'storefront';
  const codeSlug = (category?.code ?? 'unknown').toLowerCase();
  const inactive = partner.status !== 'KOOPERIERT' ? ' is-inactive' : '';
  const categoryClass = ` map-marker--${codeSlug}`;
  const badge =
    order != null ? `<span class="map-marker__badge">${order}</span>` : '';
  return L.divIcon({
    className: `map-marker${categoryClass}${inactive}`,
    html: `
        <div class="map-marker__pin">
          <span class="material-symbols-outlined">${symbol}</span>
          ${badge}
        </div>
        <svg class="map-marker__tail" viewBox="0 0 12 10" aria-hidden="true">
          <path d="M0 0 H12 L6 10 Z"/>
        </svg>
      `,
    iconSize: [36, 44],
    iconAnchor: [18, 42],
    popupAnchor: [0, -38],
  });
}

export function buildDistributionPointMarkerIcon(): L.DivIcon {
  return L.divIcon({
    className: 'map-marker map-marker--distribution-point',
    html: `
        <div class="map-marker__pin">
          <span class="material-symbols-outlined">distance</span>
        </div>
        <svg class="map-marker__tail" viewBox="0 0 12 10" aria-hidden="true">
          <path d="M0 0 H12 L6 10 Z"/>
        </svg>
      `,
    iconSize: [36, 44],
    iconAnchor: [18, 42],
    popupAnchor: [0, -38],
  });
}
