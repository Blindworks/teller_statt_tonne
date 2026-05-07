export type NoteVisibility = 'INTERNAL' | 'SHARED';

export interface PartnerNote {
  id: number;
  partnerId: number;
  body: string;
  visibility: NoteVisibility;
  createdAt: string;
  authorUserId: number | null;
  authorDisplayName: string;
  deleted: boolean;
}

export interface CreatePartnerNoteRequest {
  body: string;
  visibility: NoteVisibility;
}
