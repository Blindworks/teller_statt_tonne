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
  categoryId: number | null;
  street: string;
  postalCode: string;
  city: string;
  logoUrl: string | null;
  contact: Contact;
  retterContact: Contact;
  pickupSlots: PickupSlot[];
  status: Status;
  latitude: number | null;
  longitude: number | null;
  parkingInfo: string | null;
  accessInstructions: string | null;
  pickupProcedure: string | null;
  onSiteContactNote: string | null;
  deliveryNoteInfo: string | null;
  depositInfo: string | null;
  wasteDisposalInfo: string | null;
  materialInfo: string | null;
  preferredFoodCategoryIds: number[];
  liabilityWaiverSigned: boolean;
  liabilityWaiverSignedOn: string | null;
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

export const STATUS_LABELS: Record<Status, string> = {
  KEIN_KONTAKT: 'Kein Kontakt',
  VERHANDLUNGEN_LAUFEN: 'Verhandlungen laufen',
  WILL_NICHT_KOOPERIEREN: 'Will nicht kooperieren',
  KOOPERIERT: 'Kooperiert mit uns',
  KOOPERIERT_FOODSHARING: 'Wird alternativ berettet',
  SPENDET_AN_TAFEL: 'spendet Gemeinnützig',
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
    categoryId: null,
    street: '',
    postalCode: '',
    city: '',
    logoUrl: null,
    contact: { name: '', email: '', phone: '' },
    retterContact: { name: '', email: '', phone: '' },
    pickupSlots: [],
    status: 'KEIN_KONTAKT',
    latitude: null,
    longitude: null,
    parkingInfo: null,
    accessInstructions: null,
    pickupProcedure: null,
    onSiteContactNote: null,
    deliveryNoteInfo: null,
    depositInfo: null,
    wasteDisposalInfo: null,
    materialInfo: null,
    preferredFoodCategoryIds: [],
    liabilityWaiverSigned: false,
    liabilityWaiverSignedOn: null,
  };
}
