export type Weekday =
  | 'MONDAY'
  | 'TUESDAY'
  | 'WEDNESDAY'
  | 'THURSDAY'
  | 'FRIDAY'
  | 'SATURDAY'
  | 'SUNDAY';

export const WEEKDAYS: Weekday[] = [
  'MONDAY',
  'TUESDAY',
  'WEDNESDAY',
  'THURSDAY',
  'FRIDAY',
  'SATURDAY',
  'SUNDAY',
];

export const WEEKDAY_LABELS: Record<Weekday, string> = {
  MONDAY: 'Montag',
  TUESDAY: 'Dienstag',
  WEDNESDAY: 'Mittwoch',
  THURSDAY: 'Donnerstag',
  FRIDAY: 'Freitag',
  SATURDAY: 'Samstag',
  SUNDAY: 'Sonntag',
};

export interface OperatorRef {
  id: number;
  displayName: string;
}

export interface OpeningSlot {
  weekday: Weekday;
  startTime: string;
  endTime: string;
}

export interface DistributionPoint {
  id: number | null;
  name: string;
  description: string | null;
  street: string | null;
  postalCode: string | null;
  city: string | null;
  latitude: number | null;
  longitude: number | null;
  operators: OperatorRef[];
  openingSlots: OpeningSlot[];
}

export function emptyDistributionPoint(): DistributionPoint {
  return {
    id: null,
    name: '',
    description: '',
    street: '',
    postalCode: '',
    city: '',
    latitude: null,
    longitude: null,
    operators: [],
    openingSlots: [],
  };
}
