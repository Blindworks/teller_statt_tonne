export interface EventContact {
  name: string | null;
  email: string | null;
  phone: string | null;
}

export interface CharityEvent {
  id: number | null;
  name: string;
  description: string | null;
  startDate: string;
  endDate: string;
  street: string | null;
  postalCode: string | null;
  city: string | null;
  latitude: number | null;
  longitude: number | null;
  logoUrl: string | null;
  contact: EventContact;
}

export function emptyEvent(): CharityEvent {
  return {
    id: null,
    name: '',
    description: '',
    startDate: '',
    endDate: '',
    street: '',
    postalCode: '',
    city: '',
    latitude: null,
    longitude: null,
    logoUrl: null,
    contact: { name: '', email: '', phone: '' },
  };
}

export type EventScope = 'active' | 'past' | 'all';
