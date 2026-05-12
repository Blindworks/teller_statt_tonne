export interface FoodCategory {
  id: number | null;
  name: string;
  emoji: string | null;
  colorHex: string | null;
  sortOrder: number;
  active: boolean;
}

export function emptyFoodCategory(): FoodCategory {
  return { id: null, name: '', emoji: '', colorHex: null, sortOrder: 0, active: true };
}
