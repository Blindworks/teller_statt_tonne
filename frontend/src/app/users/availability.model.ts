import { Weekday } from '../partners/partner.model';

export interface UserAvailability {
  id: number | null;
  userId: number;
  weekday: Weekday;
  startTime: string;
  endTime: string;
}
