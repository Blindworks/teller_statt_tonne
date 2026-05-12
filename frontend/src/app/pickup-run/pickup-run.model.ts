export type PickupRunStatus = 'IN_PROGRESS' | 'COMPLETED' | 'ABORTED';
export type DistributionPostStatus = 'FRESH' | 'PARTIALLY_AVAILABLE' | 'EMPTY';

export interface PickupRunItem {
  id: number;
  foodCategoryId: number | null;
  customLabel: string | null;
  takenAt: string | null;
}

export interface PickupRun {
  id: number;
  pickupId: number;
  retterId: number;
  startedAt: string;
  completedAt: string | null;
  status: PickupRunStatus;
  distributionPointId: number | null;
  notes: string | null;
  items: PickupRunItem[];
}

export interface DistributionPostPhoto {
  id: number;
  imageUrl: string;
  uploadedById: number;
  uploadedAt: string;
}

export interface DistributionPostItem {
  id: number;
  foodCategoryId: number | null;
  customLabel: string | null;
  takenAt: string | null;
}

export interface DistributionPost {
  id: number;
  distributionPointId: number;
  pickupRunId: number;
  partnerId: number | null;
  postedById: number;
  createdAt: string;
  updatedAt: string;
  status: DistributionPostStatus;
  notes: string | null;
  photos: DistributionPostPhoto[];
  items: DistributionPostItem[];
}

export interface CompleteResponse {
  run: PickupRun;
  post: DistributionPost;
}
