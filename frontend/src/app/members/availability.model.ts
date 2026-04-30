import { Weekday } from '../partners/partner.model';

export interface MemberAvailability {
  id: number | null;
  memberId: number;
  weekday: Weekday;
  startTime: string;
  endTime: string;
}
