export type PickupStatus = 'SCHEDULED' | 'COMPLETED' | 'CANCELLED';

export interface PickupAssignment {
  memberId: number;
  memberName: string | null;
  memberAvatarUrl: string | null;
}

export interface Pickup {
  id: number | null;
  partnerId: number | null;
  partnerName: string | null;
  partnerCategoryId: number | null;
  partnerStreet: string | null;
  partnerCity: string | null;
  partnerLogoUrl: string | null;
  eventId: number | null;
  eventName: string | null;
  eventLogoUrl: string | null;
  date: string;
  startTime: string;
  endTime: string;
  status: PickupStatus;
  capacity: number;
  assignments: PickupAssignment[];
  notes: string | null;
  savedKg: number | null;
}

export const PICKUP_STATUS_LABELS: Record<PickupStatus, string> = {
  SCHEDULED: 'Geplant',
  COMPLETED: 'Abgeschlossen',
  CANCELLED: 'Abgesagt',
};

export function emptyPickup(): Pickup {
  return {
    id: null,
    partnerId: null,
    partnerName: null,
    partnerCategoryId: null,
    partnerStreet: null,
    partnerCity: null,
    partnerLogoUrl: null,
    eventId: null,
    eventName: null,
    eventLogoUrl: null,
    date: new Date().toISOString().slice(0, 10),
    startTime: '18:00',
    endTime: '18:30',
    status: 'SCHEDULED',
    capacity: 2,
    assignments: [],
    notes: null,
    savedKg: null,
  };
}
