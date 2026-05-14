export type HygieneCertificateStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'REPLACED';

export type HygieneValidityStatus = 'valid' | 'expiring-soon' | 'expired' | null;

export const HYGIENE_CERTIFICATE_STATUS_LABELS: Record<HygieneCertificateStatus, string> = {
  PENDING: 'Ausstehend',
  APPROVED: 'Genehmigt',
  REJECTED: 'Abgelehnt',
  REPLACED: 'Ersetzt',
};

export interface HygieneCertificate {
  id: number;
  userId: number;
  userFirstName: string | null;
  userLastName: string | null;
  userEmail: string | null;
  userPhotoUrl: string | null;
  mimeType: string;
  originalFilename: string | null;
  fileSizeBytes: number;
  issuedDate: string; // ISO date
  expiryDate: string; // ISO date
  validityStatus: HygieneValidityStatus;
  daysUntilExpiry: number | null;
  status: HygieneCertificateStatus;
  rejectionReason: string | null;
  decidedByUserId: number | null;
  decidedByDisplayName: string | null;
  decidedAt: string | null;
  createdAt: string;
  updatedAt: string;
}
