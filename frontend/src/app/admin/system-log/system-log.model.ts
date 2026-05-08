export type SystemLogCategory = 'AUTH' | 'USER_MGMT' | 'ADMIN_ACTION' | 'SYSTEM';

export type SystemLogSeverity = 'INFO' | 'WARN' | 'ERROR';

export type SystemLogEventType =
  | 'LOGIN_SUCCESS'
  | 'LOGIN_FAILED'
  | 'LOGOUT'
  | 'PASSWORD_RESET_REQUESTED'
  | 'PASSWORD_RESET_COMPLETED'
  | 'PASSWORD_CHANGED'
  | 'USER_CREATED'
  | 'USER_UPDATED'
  | 'USER_DELETED'
  | 'USER_ROLES_CHANGED'
  | 'ROLE_CREATED'
  | 'ROLE_UPDATED'
  | 'ROLE_DELETED'
  | 'HYGIENE_CERTIFICATE_APPROVED'
  | 'HYGIENE_CERTIFICATE_REJECTED'
  | 'PARTNER_APPLICATION_APPROVED'
  | 'PARTNER_APPLICATION_REJECTED'
  | 'STORE_DELETED'
  | 'STORE_RESTORED'
  | 'STORE_MEMBER_ASSIGNED'
  | 'UNHANDLED_EXCEPTION'
  | 'MAIL_DELIVERY_FAILED';

export interface SystemLogEntry {
  id: number;
  createdAt: string;
  eventType: SystemLogEventType;
  severity: SystemLogSeverity;
  category: SystemLogCategory;
  actorUserId: number | null;
  actorEmail: string | null;
  targetType: string | null;
  targetId: number | null;
  message: string;
  details: string | null;
  ipAddress: string | null;
  userAgent: string | null;
}

export interface SystemLogPage {
  content: SystemLogEntry[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}

export interface SystemLogEventTypeMeta {
  categories: SystemLogCategory[];
  severities: SystemLogSeverity[];
  eventTypes: SystemLogEventType[];
}

export interface SystemLogFilter {
  category?: SystemLogCategory | null;
  eventType?: SystemLogEventType | null;
  severity?: SystemLogSeverity | null;
  actorUserId?: number | null;
  from?: string | null;
  to?: string | null;
  search?: string | null;
}

export const SYSTEM_LOG_CATEGORY_LABELS: Record<SystemLogCategory, string> = {
  AUTH: 'Authentifizierung',
  USER_MGMT: 'Nutzer & Rollen',
  ADMIN_ACTION: 'Admin-Aktion',
  SYSTEM: 'System',
};

export const SYSTEM_LOG_SEVERITY_LABELS: Record<SystemLogSeverity, string> = {
  INFO: 'Info',
  WARN: 'Warnung',
  ERROR: 'Fehler',
};

export const SYSTEM_LOG_EVENT_TYPE_LABELS: Record<SystemLogEventType, string> = {
  LOGIN_SUCCESS: 'Login erfolgreich',
  LOGIN_FAILED: 'Login fehlgeschlagen',
  LOGOUT: 'Logout',
  PASSWORD_RESET_REQUESTED: 'Passwort-Reset angefordert',
  PASSWORD_RESET_COMPLETED: 'Passwort-Reset abgeschlossen',
  PASSWORD_CHANGED: 'Passwort geändert',
  USER_CREATED: 'Nutzer angelegt',
  USER_UPDATED: 'Nutzer aktualisiert',
  USER_DELETED: 'Nutzer gelöscht',
  USER_ROLES_CHANGED: 'Rollen geändert',
  ROLE_CREATED: 'Rolle angelegt',
  ROLE_UPDATED: 'Rolle aktualisiert',
  ROLE_DELETED: 'Rolle gelöscht',
  HYGIENE_CERTIFICATE_APPROVED: 'Hygienezertifikat genehmigt',
  HYGIENE_CERTIFICATE_REJECTED: 'Hygienezertifikat abgelehnt',
  PARTNER_APPLICATION_APPROVED: 'Bewerbung angenommen',
  PARTNER_APPLICATION_REJECTED: 'Bewerbung abgelehnt',
  STORE_DELETED: 'Betrieb gelöscht',
  STORE_RESTORED: 'Betrieb wiederhergestellt',
  STORE_MEMBER_ASSIGNED: 'Nutzer zu Betrieb zugeordnet',
  UNHANDLED_EXCEPTION: 'Unbehandelte Exception',
  MAIL_DELIVERY_FAILED: 'Mail-Versand fehlgeschlagen',
};
