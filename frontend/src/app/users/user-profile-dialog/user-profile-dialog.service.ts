import { Injectable, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class UserProfileDialogService {
  private readonly _userId = signal<number | null>(null);
  readonly userId = this._userId.asReadonly();

  open(userId: number): void {
    if (userId == null) return;
    this._userId.set(userId);
  }

  close(): void {
    this._userId.set(null);
  }
}
