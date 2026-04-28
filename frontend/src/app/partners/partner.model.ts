export type Category = 'BAKERY' | 'SUPERMARKET' | 'CAFE' | 'RESTAURANT';
export type Status = 'ACTIVE' | 'INACTIVE';
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
}

export interface Partner {
  id: string | null;
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
};

export const CATEGORY_ICONS: Record<Category, string> = {
  BAKERY: 'bakery_dining',
  SUPERMARKET: 'shopping_basket',
  CAFE: 'restaurant',
  RESTAURANT: 'local_mall',
};

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
    pickupSlots: WEEKDAYS.map((w) => ({
      weekday: w.value,
      startTime: '09:00',
      endTime: '10:00',
      active: false,
    })),
    status: 'ACTIVE',
    latitude: null,
    longitude: null,
  };
}
