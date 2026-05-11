import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { environment } from '../../environments/environment';
import { PartnerCategory } from '../admin/partner-categories/partner-category.model';

/**
 * Zentrale Quelle für Betrieb-Kategorien im Frontend.
 *
 * Lädt aktive Kategorien einmalig (per APP_INITIALIZER beim App-Start) und
 * stellt sie als Signal bereit. Komponenten greifen ausschließlich über
 * `categories()`, `labelFor()`, `iconFor()`, `byId()` zu — kein Hartcoding.
 */
@Injectable({ providedIn: 'root' })
export class PartnerCategoryRegistry {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/api/partner-categories`;

  private readonly _categories = signal<PartnerCategory[]>([]);

  readonly categories = this._categories.asReadonly();
  readonly byIdMap = computed(() => {
    const map = new Map<number, PartnerCategory>();
    for (const c of this._categories()) {
      if (c.id != null) map.set(c.id, c);
    }
    return map;
  });
  readonly byCodeMap = computed(() => {
    const map = new Map<string, PartnerCategory>();
    for (const c of this._categories()) map.set(c.code, c);
    return map;
  });

  load(): Observable<PartnerCategory[]> {
    return this.http
      .get<PartnerCategory[]>(this.baseUrl)
      .pipe(tap((list) => this._categories.set(list ?? [])));
  }

  reload(): void {
    this.load().subscribe({ error: () => void 0 });
  }

  byId(id: number | null | undefined): PartnerCategory | null {
    if (id == null) return null;
    return this.byIdMap().get(id) ?? null;
  }

  byCode(code: string | null | undefined): PartnerCategory | null {
    if (!code) return null;
    return this.byCodeMap().get(code) ?? null;
  }

  labelForId(id: number | null | undefined): string {
    const c = this.byId(id);
    return c?.label ?? '';
  }

  iconForId(id: number | null | undefined): string {
    const c = this.byId(id);
    return c?.icon ?? 'storefront';
  }
}
