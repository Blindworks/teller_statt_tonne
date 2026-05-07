export type ApplicationStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'WITHDRAWN';

export interface PartnerApplication {
  id: number;
  partnerId: number;
  partnerName: string;
  partnerStreet: string | null;
  partnerPostalCode: string | null;
  partnerCity: string | null;
  userId: number;
  userFirstName: string;
  userLastName: string;
  userEmail: string;
  userPhotoUrl: string | null;
  status: ApplicationStatus;
  message: string | null;
  decisionReason: string | null;
  decidedByUserId: number | null;
  decidedByDisplayName: string | null;
  createdAt: string;
  updatedAt: string;
  decidedAt: string | null;
}

export const APPLICATION_STATUS_LABELS: Record<ApplicationStatus, string> = {
  PENDING: 'Offen',
  APPROVED: 'Angenommen',
  REJECTED: 'Abgelehnt',
  WITHDRAWN: 'Zurückgezogen',
};
