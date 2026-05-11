export interface PartnerCategory {
  id: number | null;
  code: string;
  label: string;
  icon: string;
  orderIndex: number;
  active: boolean;
}

export function emptyPartnerCategory(): PartnerCategory {
  return {
    id: null,
    code: '',
    label: '',
    icon: 'storefront',
    orderIndex: 0,
    active: true,
  };
}

/**
 * Vorschlagsliste für den Icon-Picker im Admin-Formular.
 * Werte sind Material-Symbols-Namen.
 */
export const CURATED_ICONS: ReadonlyArray<string> = [
  'bakery_dining',
  'shopping_basket',
  'restaurant',
  'local_mall',
  'kebab_dining',
  'local_dining',
  'lunch_dining',
  'fastfood',
  'icecream',
  'cake',
  'coffee',
  'liquor',
  'eco',
  'agriculture',
  'storefront',
];
