import { Category } from '../partners/partner.model';

export type PickupStatus = 'SCHEDULED' | 'COMPLETED' | 'CANCELLED';

export interface PickupAssignment {
  memberId: string;
  memberName: string | null;
  memberAvatarUrl: string | null;
}

export interface Pickup {
  id: string | null;
  partnerId: string;
  partnerName: string | null;
  partnerCategory: Category | null;
  partnerStreet: string | null;
  partnerCity: string | null;
  partnerLogoUrl: string | null;
  date: string;
  startTime: string;
  endTime: string;
  status: PickupStatus;
  capacity: number;
  assignments: PickupAssignment[];
  notes: string | null;
}

export const PICKUP_STATUS_LABELS: Record<PickupStatus, string> = {
  SCHEDULED: 'Geplant',
  COMPLETED: 'Abgeschlossen',
  CANCELLED: 'Abgesagt',
};

export function emptyPickup(): Pickup {
  return {
    id: null,
    partnerId: '',
    partnerName: null,
    partnerCategory: null,
    partnerStreet: null,
    partnerCity: null,
    partnerLogoUrl: null,
    date: new Date().toISOString().slice(0, 10),
    startTime: '18:00',
    endTime: '18:30',
    status: 'SCHEDULED',
    capacity: 2,
    assignments: [],
    notes: null,
  };
}
