import { OnlineStatus, RoleName, UserStatus } from '../../users/user.model';

export interface StoreMember {
  id: number;
  firstName: string;
  lastName: string;
  roles: RoleName[];
  email: string | null;
  city: string | null;
  photoUrl: string | null;
  onlineStatus: OnlineStatus;
  status: UserStatus;
  lastPickupDate: string | null;
  totalSavedKg: number | null;
  pickupCount: number;
}
