export type MemberType = 'BOTSCHAFTER' | 'FOODSAVER' | 'NEW_MEMBER';
export type OnlineStatus = 'ONLINE' | 'AWAY' | 'ON_BREAK' | 'OFFLINE';
export type MemberStatus = 'ACTIVE' | 'PENDING' | 'INACTIVE';

export interface Member {
  id: string | null;
  firstName: string;
  lastName: string;
  type: MemberType;
  roleTitle: string;
  email: string;
  phone: string;
  city: string;
  photoUrl: string | null;
  onlineStatus: OnlineStatus;
  status: MemberStatus;
  tags: string[];
}

export const MEMBER_TYPES: ReadonlyArray<{ value: MemberType; label: string }> = [
  { value: 'BOTSCHAFTER', label: 'Botschafter' },
  { value: 'FOODSAVER', label: 'Foodsaver' },
  { value: 'NEW_MEMBER', label: 'Neues Mitglied' },
];

export const MEMBER_TYPE_LABELS: Record<MemberType, string> = {
  BOTSCHAFTER: 'Botschafter',
  FOODSAVER: 'Foodsaver',
  NEW_MEMBER: 'Neues Mitglied',
};

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

export function emptyMember(): Member {
  return {
    id: null,
    firstName: '',
    lastName: '',
    type: 'FOODSAVER',
    roleTitle: '',
    email: '',
    phone: '',
    city: '',
    photoUrl: null,
    onlineStatus: 'OFFLINE',
    status: 'ACTIVE',
    tags: [],
  };
}
