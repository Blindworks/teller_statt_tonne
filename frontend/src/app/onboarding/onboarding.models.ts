export interface OnboardingStatus {
  hygieneCompleted: boolean;
  introductionCompleted: boolean;
  profileCompleted: boolean;
  agreementCompleted: boolean;
  testPickupCompleted: boolean;
  allCompleted: boolean;
  activated: boolean;
  introductionBookingId: number | null;
  introductionSlotId: number | null;
}

export interface IntroductionBookingInfo {
  bookingId: number;
  userId: number;
  firstName: string | null;
  lastName: string | null;
  email: string | null;
  testUser: boolean;
}

export interface IntroductionSlot {
  id: number;
  date: string;
  startTime: string;
  endTime: string;
  location: string | null;
  capacity: number;
  bookedCount: number;
  notes: string | null;
  bookedByMe: boolean;
  bookings: IntroductionBookingInfo[];
}

export interface IntroductionSlotRequest {
  date: string;
  startTime: string;
  endTime: string;
  location?: string | null;
  capacity?: number | null;
  notes?: string | null;
}
