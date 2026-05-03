import { Injectable, signal } from '@angular/core';
import { Partner } from '../../partners/partner.model';

@Injectable({ providedIn: 'root' })
export class StoreDetailDialogService {
  private readonly _partner = signal<Partner | null>(null);
  readonly partner = this._partner.asReadonly();

  open(partner: Partner): void {
    if (!partner) return;
    this._partner.set(partner);
  }

  close(): void {
    this._partner.set(null);
  }
}
