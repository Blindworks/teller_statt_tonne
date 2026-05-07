import { Injectable, signal } from '@angular/core';

export type ConfirmTone = 'primary' | 'danger';

export interface ConfirmOptions {
  title: string;
  message: string;
  confirmLabel?: string;
  cancelLabel?: string;
  tone?: ConfirmTone;
}

interface ConfirmRequest extends ConfirmOptions {
  resolve: (result: boolean) => void;
}

@Injectable({ providedIn: 'root' })
export class ConfirmDialogService {
  private readonly _request = signal<ConfirmRequest | null>(null);
  readonly request = this._request.asReadonly();

  ask(options: ConfirmOptions): Promise<boolean> {
    return new Promise((resolve) => {
      const previous = this._request();
      if (previous) previous.resolve(false);
      this._request.set({
        confirmLabel: 'Bestätigen',
        cancelLabel: 'Abbrechen',
        tone: 'primary',
        ...options,
        resolve,
      });
    });
  }

  resolve(result: boolean): void {
    const current = this._request();
    if (!current) return;
    this._request.set(null);
    current.resolve(result);
  }
}
