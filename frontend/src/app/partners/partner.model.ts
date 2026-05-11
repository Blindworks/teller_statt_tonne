export type Category = 'BAKERY' | 'SUPERMARKET' | 'CAFE' | 'RESTAURANT' | 'BUTCHER';
export type Status =
  | 'KEIN_KONTAKT'
  | 'VERHANDLUNGEN_LAUFEN'
  | 'WILL_NICHT_KOOPERIEREN'
  | 'KOOPERIERT'
  | 'KOOPERIERT_FOODSHARING'
  | 'SPENDET_AN_TAFEL'
  | 'EXISTIERT_NICHT_MEHR';
export type Weekday =
  | 'MONDAY'
  | 'TUESDAY'
  | 'WEDNESDAY'
  | 'THURSDAY'
  | 'FRIDAY'
  | 'SATURDAY'
  | 'SUNDAY';

export interface Contact {
  name: string;
  email: string;
  phone: string;
}

export interface PickupSlot {
  weekday: Weekday;
  startTime: string;
  endTime: string;
  active: boolean;
  capacity: number;
  expectedKg: number | null;
  availableMemberCount?: number;
}

export interface Partner {
  id: number | null;
  name: string;
  category: Category;
  street: string;
  postalCode: string;
  city: string;
  logoUrl: string | null;
  contact: Contact;
  pickupSlots: PickupSlot[];
  status: Status;
  latitude: number | null;
  longitude: number | null;
}

export const WEEKDAYS: ReadonlyArray<{ value: Weekday; label: string }> = [
  { value: 'MONDAY', label: 'Montag' },
  { value: 'TUESDAY', label: 'Dienstag' },
  { value: 'WEDNESDAY', label: 'Mittwoch' },
  { value: 'THURSDAY', label: 'Donnerstag' },
  { value: 'FRIDAY', label: 'Freitag' },
  { value: 'SATURDAY', label: 'Samstag' },
  { value: 'SUNDAY', label: 'Sonntag' },
];

export const CATEGORY_LABELS: Record<Category, string> = {
  BAKERY: 'Bäckerei',
  SUPERMARKET: 'Supermarkt',
  CAFE: 'Café',
  RESTAURANT: 'Restaurant',
  BUTCHER: 'Metzgerei',
};

export const CATEGORY_ICONS: Record<Category, string> = {
  BAKERY: 'bakery_dining',
  SUPERMARKET: 'shopping_basket',
  CAFE: 'restaurant',
  RESTAURANT: 'local_mall',
  BUTCHER: 'kebab_dining',
};

export const STATUS_LABELS: Record<Status, string> = {
  KEIN_KONTAKT: 'Kein Kontakt',
  VERHANDLUNGEN_LAUFEN: 'Verhandlungen laufen',
  WILL_NICHT_KOOPERIEREN: 'Will nicht kooperieren',
  KOOPERIERT: 'Kooperiert mit uns',
  KOOPERIERT_FOODSHARING: 'Kooperiert mit Foodsharing',
  SPENDET_AN_TAFEL: 'Spendet an Tafel etc.',
  EXISTIERT_NICHT_MEHR: 'Existiert nicht mehr',
};

export const STATUS_ORDER: ReadonlyArray<Status> = [
  'KEIN_KONTAKT',
  'VERHANDLUNGEN_LAUFEN',
  'WILL_NICHT_KOOPERIEREN',
  'KOOPERIERT',
  'KOOPERIERT_FOODSHARING',
  'SPENDET_AN_TAFEL',
  'EXISTIERT_NICHT_MEHR',
];

export function emptyPartner(): Partner {
  return {
    id: null,
    name: '',
    category: 'BAKERY',
    street: '',
    postalCode: '',
    city: '',
    logoUrl: null,
    contact: { name: '', email: '', phone: '' },
    pickupSlots: [],
    status: 'KEIN_KONTAKT',
    latitude: null,
    longitude: null,
  };
}
