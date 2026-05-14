import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, firstValueFrom, map, shareReplay, tap } from 'rxjs';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class PermissionsService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiBaseUrl}/api/me/features`;

  private readonly featuresSignal = signal<ReadonlySet<string>>(new Set());
  private readonly loadedSignal = signal<boolean>(false);
  private inflight: Observable<ReadonlySet<string>> | null = null;

  readonly features = this.featuresSignal.asReadonly();
  readonly loaded = this.loadedSignal.asReadonly();
  readonly featuresList = computed(() => Array.from(this.featuresSignal()).sort());

  has(key: string): boolean {
    return this.featuresSignal().has(key);
  }

  /** Lädt die Feature-Keys des eingeloggten Users vom Backend. Idempotent während Flug. */
  load(): Observable<ReadonlySet<string>> {
    if (this.inflight) return this.inflight;
    const obs = this.http.get<string[]>(this.url).pipe(
      map((keys) => new Set(keys ?? []) as ReadonlySet<string>),
      tap((set) => {
        this.featuresSignal.set(set);
        this.loadedSignal.set(true);
      }),
      shareReplay({ bufferSize: 1, refCount: false }),
    );
    this.inflight = obs;
    obs.subscribe({
      complete: () => {
        this.inflight = null;
      },
      error: () => {
        this.inflight = null;
      },
    });
    return obs;
  }

  /** Stellt sicher, dass Features geladen sind. */
  ensureLoaded(): Promise<ReadonlySet<string>> {
    if (this.loadedSignal()) {
      return Promise.resolve(this.featuresSignal());
    }
    return firstValueFrom(this.load());
  }

  clear(): void {
    this.featuresSignal.set(new Set());
    this.loadedSignal.set(false);
    this.inflight = null;
  }
}
