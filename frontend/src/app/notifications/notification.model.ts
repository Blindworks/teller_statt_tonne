export type NotificationType = 'PICKUP_UNASSIGNED' | 'PICKUP_CANCELLED' | 'PICKUP_COMPLETED';

export interface AppNotification {
  id: number;
  type: NotificationType;
  title: string;
  body: string;
  relatedPickupId: number | null;
  relatedPartnerId: number | null;
  actorUserId: number | null;
  createdAt: string;
  readAt: string | null;
}
