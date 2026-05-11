import { PickupAssignment } from '../pickups/pickup.model';

export interface DaySlot {
  pickupId: number | null;
  partnerId: number;
  partnerName: string;
  partnerCategoryId: number | null;
  partnerStreet: string | null;
  partnerCity: string | null;
  partnerLogoUrl: string | null;
  date: string;
  startTime: string;
  endTime: string;
  capacity: number;
  assignments: PickupAssignment[];
  isTemplate: boolean;
  currentUserAssigned: boolean;
}
