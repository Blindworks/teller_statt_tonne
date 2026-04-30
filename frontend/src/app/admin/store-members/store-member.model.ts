import { OnlineStatus, MemberStatus, MemberRole } from '../../members/member.model';

export interface StoreMember {
  id: string;
  firstName: string;
  lastName: string;
  role: MemberRole;
  email: string | null;
  city: string | null;
  photoUrl: string | null;
  onlineStatus: OnlineStatus;
  status: MemberStatus;
  lastPickupDate: string | null;
  totalSavedKg: number | null;
  pickupCount: number;
}
