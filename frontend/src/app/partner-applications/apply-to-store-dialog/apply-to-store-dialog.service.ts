import { Injectable, signal } from '@angular/core';

export interface ApplyDialogTarget {
  partnerId: number;
  partnerName: string;
}

@Injectable({ providedIn: 'root' })
export class ApplyToStoreDialogService {
  private readonly _target = signal<ApplyDialogTarget | null>(null);
  readonly target = this._target.asReadonly();

  open(target: ApplyDialogTarget): void {
    this._target.set(target);
  }

  close(): void {
    this._target.set(null);
  }
}
