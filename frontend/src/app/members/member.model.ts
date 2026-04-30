export type MemberRole = string;
export type OnlineStatus = 'ONLINE' | 'AWAY' | 'ON_BREAK' | 'OFFLINE';
export type MemberStatus = 'ACTIVE' | 'PENDING' | 'INACTIVE';

export interface MemberRoleOption {
  value: MemberRole;
  label: string;
}

export interface Member {
  id: string | null;
  firstName: string;
  lastName: string;
  role: MemberRole;
  email: string;
  phone: string;
  city: string;
  photoUrl: string | null;
  onlineStatus: OnlineStatus;
  status: MemberStatus;
  tags: string[];
}

export const ONLINE_STATUS_LABELS: Record<OnlineStatus, string> = {
  ONLINE: 'Online',
  AWAY: 'Abwesend',
  ON_BREAK: 'Pause',
  OFFLINE: 'Offline',
};

export const MEMBER_STATUS_LABELS: Record<MemberStatus, string> = {
  ACTIVE: 'Aktiv',
  PENDING: 'Ausstehend',
  INACTIVE: 'Inaktiv',
};

export const ONLINE_STATUSES: OnlineStatus[] = ['ONLINE', 'AWAY', 'ON_BREAK', 'OFFLINE'];
export const MEMBER_STATUSES: MemberStatus[] = ['ACTIVE', 'PENDING', 'INACTIVE'];

export function emptyMember(defaultRole: MemberRole = 'FOODSAVER'): Member {
  return {
    id: null,
    firstName: '',
    lastName: '',
    role: defaultRole,
    email: '',
    phone: '',
    city: '',
    photoUrl: null,
    onlineStatus: 'OFFLINE',
    status: 'ACTIVE',
    tags: [],
  };
}
