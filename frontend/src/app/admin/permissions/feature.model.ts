export interface Feature {
  id: number;
  key: string;
  label: string;
  description: string | null;
  category: string | null;
  sortOrder: number;
}

export interface FeatureRequest {
  key: string;
  label: string;
  description?: string | null;
  category?: string | null;
  sortOrder?: number | null;
}

export interface RoleFeatureAssignment {
  featureIds: number[];
}
