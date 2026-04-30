export type Role = 'ADMINISTRATOR' | 'BOTSCHAFTER' | 'RETTER' | 'NEW_MEMBER';
export type OnlineStatus = 'ONLINE' | 'AWAY' | 'ON_BREAK' | 'OFFLINE';
export type UserStatus = 'ACTIVE' | 'PENDING' | 'INACTIVE';

export interface RoleOption {
  value: Role;
  label: string;
}

export interface User {
  id: number | null;
  firstName: string;
  lastName: string;
  role: Role;
  email: string;
  phone: string | null;
  street: string | null;
  postalCode: string | null;
  city: string | null;
  country: string | null;
  photoUrl: string | null;
  onlineStatus: OnlineStatus;
  status: UserStatus;
  tags: string[];
}

export const ONLINE_STATUS_LABELS: Record<OnlineStatus, string> = {
  ONLINE: 'Online',
  AWAY: 'Abwesend',
  ON_BREAK: 'Pause',
  OFFLINE: 'Offline',
};

export const USER_STATUS_LABELS: Record<UserStatus, string> = {
  ACTIVE: 'Aktiv',
  PENDING: 'Ausstehend',
  INACTIVE: 'Inaktiv',
};

export const ONLINE_STATUSES: OnlineStatus[] = ['ONLINE', 'AWAY', 'ON_BREAK', 'OFFLINE'];
export const USER_STATUSES: UserStatus[] = ['ACTIVE', 'PENDING', 'INACTIVE'];

export function emptyUser(defaultRole: Role = 'RETTER'): User {
  return {
    id: null,
    firstName: '',
    lastName: '',
    role: defaultRole,
    email: '',
    phone: '',
    street: '',
    postalCode: '',
    city: '',
    country: '',
    photoUrl: null,
    onlineStatus: 'OFFLINE',
    status: 'ACTIVE',
    tags: [],
  };
}
