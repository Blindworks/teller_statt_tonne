import { Role } from '../admin/roles/role.model';

export interface Appointment {
  id: number;
  title: string;
  description: string | null;
  startTime: string;
  endTime: string;
  location: string | null;
  attachmentUrl: string | null;
  isPublic: boolean;
  createdById: number;
  targetRoles: Role[];
  read: boolean;
  canEdit: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface AppointmentInput {
  title: string;
  description: string | null;
  startTime: string;
  endTime: string;
  location: string | null;
  attachmentUrl: string | null;
  isPublic: boolean;
  targetRoleIds: number[];
}

export interface PublicAppointment {
  id: number;
  title: string;
  description: string | null;
  startTime: string;
  endTime: string;
  location: string | null;
  attachmentUrl: string | null;
}

export function emptyAppointmentInput(): AppointmentInput {
  return {
    title: '',
    description: '',
    startTime: '',
    endTime: '',
    location: '',
    attachmentUrl: '',
    isPublic: false,
    targetRoleIds: [],
  };
}
