export interface RescuerCardCurrentPickup {
  pickupId: number;
  partnerId: number | null;
  partnerName: string | null;
  date: string;
  startTime: string;
  endTime: string;
  active: boolean;
}

export interface RescuerCardContext {
  userId: number;
  firstName: string;
  lastName: string;
  photoUrl: string | null;
  introductionCompletedAt: string | null;
  hygieneValid: boolean;
  hygieneExpiryDate: string | null;
  currentPickup: RescuerCardCurrentPickup | null;
  generatedAt: string;
}

export interface RescuerCardTokenResponse {
  token: string;
  expiresAt: string;
}

export interface VerifyRescuerResponse {
  valid: boolean;
  reason: string | null;
  firstName: string | null;
  lastName: string | null;
  photoUrl: string | null;
  hygieneValid: boolean;
  currentPartnerName: string | null;
  pickupActive: boolean;
  generatedAt: string | null;
}
