import { Weekday } from '../partners/partner.model';

export interface MemberAvailability {
  id: string | null;
  memberId: string;
  weekday: Weekday;
  startTime: string;
  endTime: string;
}
