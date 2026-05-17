export interface SpecialPickupContact {
  name: string | null;
  email: string | null;
  phone: string | null;
}

export interface SpecialPickup {
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
  contact: SpecialPickupContact;
}

export function emptySpecialPickup(): SpecialPickup {
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

export type SpecialPickupScope = 'active' | 'past' | 'all';
