export type TicketStatus = 'OPEN' | 'IN_PROGRESS' | 'DONE' | 'REJECTED';
export type TicketCategory = 'BUG' | 'FEATURE';

export interface TicketAttachment {
  id: number;
  url: string;
  originalFilename: string | null;
  uploadedById: number;
  uploadedAt: string;
}

export interface TicketComment {
  id: number;
  body: string;
  authorId: number;
  authorName: string;
  createdAt: string;
}

export interface TicketSummary {
  id: number;
  title: string;
  category: TicketCategory;
  status: TicketStatus;
  createdById: number;
  createdByName: string;
  createdAt: string;
  updatedAt: string;
  commentCount: number;
  attachmentCount: number;
}

export interface Ticket {
  id: number;
  title: string;
  description: string | null;
  category: TicketCategory;
  status: TicketStatus;
  createdById: number;
  createdByName: string;
  createdAt: string;
  updatedAt: string;
  attachments: TicketAttachment[];
  comments: TicketComment[];
}

export interface TicketCreateRequest {
  title: string;
  description: string | null;
  category: TicketCategory;
}

export interface TicketUpdateRequest {
  title: string;
  description: string | null;
  category: TicketCategory;
}

export const TICKET_STATUS_LABEL: Record<TicketStatus, string> = {
  OPEN: 'Offen',
  IN_PROGRESS: 'In Arbeit',
  DONE: 'Erledigt',
  REJECTED: 'Abgelehnt',
};

export const TICKET_CATEGORY_LABEL: Record<TicketCategory, string> = {
  BUG: 'Bug',
  FEATURE: 'Feature',
};
