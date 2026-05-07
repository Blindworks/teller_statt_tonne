import { L } from './leaflet-global';
import { CATEGORY_ICONS, Category, Status } from '../partners/partner.model';

export function buildPartnerMarkerIcon(
  partner: { category: Category; status: Status },
  order: number | null = null,
): L.DivIcon {
  const symbol = CATEGORY_ICONS[partner.category];
  const inactive = partner.status !== 'KOOPERIERT' ? ' is-inactive' : '';
  const categoryClass = ` map-marker--${partner.category.toLowerCase()}`;
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
