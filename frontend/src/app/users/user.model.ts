export type RoleName = string;
/** @deprecated use RoleName (string) directly */
export type Role = RoleName;

export type OnlineStatus = 'ONLINE' | 'AWAY' | 'ON_BREAK' | 'OFFLINE';
export type UserStatus = 'PENDING' | 'ACTIVE' | 'PAUSED' | 'LEFT' | 'REMOVED';

export interface RoleOption {
  value: RoleName;
  label: string;
}

export interface User {
  id: number | null;
  firstName: string;
  lastName: string;
  roles: RoleName[];
  email: string;
  phone: string | null;
  street: string | null;
  postalCode: string | null;
  city: string | null;
  country: string | null;
  photoUrl: string | null;
  onlineStatus: OnlineStatus;
  status: UserStatus;
  introductionCompletedAt: string | null;
  hygieneApproved: boolean;
  hasPassword: boolean;
  tags: string[];
  agreementUploadedAt: string | null;
  testPickupCompletedAt: string | null;
  testUser: boolean;
}

export const ONLINE_STATUS_LABELS: Record<OnlineStatus, string> = {
  ONLINE: 'Online',
  AWAY: 'Abwesend',
  ON_BREAK: 'Pause',
  OFFLINE: 'Offline',
};

export const USER_STATUS_LABELS: Record<UserStatus, string> = {
  PENDING: 'Im Onboarding',
  ACTIVE: 'Aktiv',
  PAUSED: 'Pausiert',
  LEFT: 'Ausgetreten',
  REMOVED: 'Entfernt',
};

export const ONLINE_STATUSES: OnlineStatus[] = ['ONLINE', 'AWAY', 'ON_BREAK', 'OFFLINE'];
export const USER_STATUSES: UserStatus[] = ['PENDING', 'ACTIVE', 'PAUSED', 'LEFT', 'REMOVED'];

export function emptyUser(defaultRole: RoleName = 'RETTER'): User {
  return {
    id: null,
    firstName: '',
    lastName: '',
    roles: [defaultRole],
    email: '',
    phone: '',
    street: '',
    postalCode: '',
    city: '',
    country: '',
    photoUrl: null,
    onlineStatus: 'OFFLINE',
    status: 'PENDING',
    introductionCompletedAt: null,
    hygieneApproved: false,
    hasPassword: false,
    tags: [],
    agreementUploadedAt: null,
    testPickupCompletedAt: null,
    testUser: false,
  };
}

export function primaryRole(user: { roles: RoleName[] } | null | undefined): RoleName | null {
  return user?.roles?.[0] ?? null;
}

export function hasRole(user: { roles: RoleName[] } | null | undefined, role: RoleName): boolean {
  return !!user?.roles?.includes(role);
}

export function hasAnyRole(
  user: { roles: RoleName[] } | null | undefined,
  ...roles: RoleName[]
): boolean {
  if (!user?.roles?.length) return false;
  return roles.some((r) => user.roles.includes(r));
}
