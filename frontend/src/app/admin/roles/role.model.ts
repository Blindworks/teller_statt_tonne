export interface Role {
  id: number;
  name: string;
  label: string;
  description: string | null;
  sortOrder: number;
  enabled: boolean;
  userCount: number;
}

export interface RoleCreateRequest {
  name: string;
  label: string;
  description?: string | null;
  sortOrder?: number | null;
  enabled?: boolean | null;
}

export interface RoleUpdateRequest extends RoleCreateRequest {}

export function emptyRole(): Role {
  return {
    id: 0,
    name: '',
    label: '',
    description: '',
    sortOrder: 100,
    enabled: true,
    userCount: 0,
  };
}
