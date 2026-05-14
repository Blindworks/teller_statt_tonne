import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { RoleService } from '../../roles/role.service';
import { Role } from '../../roles/role.model';
import { PermissionsService } from '../../../auth/permissions.service';
import { Feature, FeatureRequest } from '../feature.model';
import { FeatureService } from '../feature.service';
import { FeatureFormComponent } from '../feature-form/feature-form';

interface FeatureGroup {
  category: string;
  features: Feature[];
}

@Component({
  selector: 'app-permissions-matrix',
  imports: [FormsModule, RouterLink, FeatureFormComponent],
  templateUrl: './permissions-matrix.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PermissionsMatrixComponent {
  private readonly featureService = inject(FeatureService);
  private readonly roleService = inject(RoleService);
  private readonly permissions = inject(PermissionsService);

  readonly roles = signal<Role[]>([]);
  readonly features = signal<Feature[]>([]);
  /** roleId -> Set of featureIds (mutable working copy). */
  readonly assignments = signal<Map<number, Set<number>>>(new Map());
  /** roleIds with unsaved changes. */
  readonly dirtyRoles = signal<Set<number>>(new Set());

  readonly loading = signal<boolean>(true);
  readonly loadError = signal<string | null>(null);
  readonly saveError = signal<string | null>(null);
  readonly busy = signal<boolean>(false);

  readonly featureFormOpen = signal<boolean>(false);
  readonly editingFeature = signal<Feature | null>(null);
  readonly confirmDeleteFeature = signal<Feature | null>(null);

  readonly groupedFeatures = computed<FeatureGroup[]>(() => {
    const map = new Map<string, Feature[]>();
    for (const f of this.features()) {
      const cat = f.category && f.category.trim().length > 0 ? f.category : 'Ohne Kategorie';
      if (!map.has(cat)) map.set(cat, []);
      map.get(cat)!.push(f);
    }
    return Array.from(map.entries())
      .sort(([a], [b]) => a.localeCompare(b, 'de'))
      .map(([category, features]) => ({
        category,
        features: features.sort((a, b) => a.sortOrder - b.sortOrder || a.id - b.id),
      }));
  });

  readonly hasDirty = computed(() => this.dirtyRoles().size > 0);

  constructor() {
    this.reloadAll();
  }

  reloadAll(): void {
    this.loading.set(true);
    this.loadError.set(null);
    this.featureService.list().subscribe({
      next: (features) => {
        this.features.set(features);
        this.roleService.list(false).subscribe({
          next: (roles) => {
            const sorted = [...roles].sort(
              (a, b) => a.sortOrder - b.sortOrder || a.name.localeCompare(b.name),
            );
            this.roles.set(sorted);
            // For each role, load its feature assignments in parallel.
            const calls = sorted.map((r) => this.featureService.getForRole(r.id));
            if (calls.length === 0) {
              this.assignments.set(new Map());
              this.loading.set(false);
              return;
            }
            forkJoin(calls).subscribe({
              next: (results) => {
                const map = new Map<number, Set<number>>();
                sorted.forEach((r, i) => {
                  map.set(r.id, new Set(results[i].featureIds ?? []));
                });
                this.assignments.set(map);
                this.dirtyRoles.set(new Set());
                this.loading.set(false);
              },
              error: () => {
                this.loadError.set('Zuordnungen konnten nicht geladen werden.');
                this.loading.set(false);
              },
            });
          },
          error: () => {
            this.loadError.set('Rollen konnten nicht geladen werden.');
            this.loading.set(false);
          },
        });
      },
      error: () => {
        this.loadError.set('Features konnten nicht geladen werden.');
        this.loading.set(false);
      },
    });
  }

  isAdminRole(role: Role): boolean {
    return role.name === 'ADMINISTRATOR';
  }

  isChecked(roleId: number, featureId: number): boolean {
    return this.assignments().get(roleId)?.has(featureId) ?? false;
  }

  toggle(role: Role, featureId: number): void {
    if (this.isAdminRole(role)) return; // gesperrt
    const map = new Map(this.assignments());
    const set = new Set(map.get(role.id) ?? []);
    if (set.has(featureId)) set.delete(featureId);
    else set.add(featureId);
    map.set(role.id, set);
    this.assignments.set(map);
    const dirty = new Set(this.dirtyRoles());
    dirty.add(role.id);
    this.dirtyRoles.set(dirty);
  }

  saveRole(role: Role): void {
    if (this.isAdminRole(role)) return;
    const ids = Array.from(this.assignments().get(role.id) ?? []);
    this.busy.set(true);
    this.saveError.set(null);
    this.featureService.setForRole(role.id, ids).subscribe({
      next: (res) => {
        const map = new Map(this.assignments());
        map.set(role.id, new Set(res.featureIds ?? []));
        this.assignments.set(map);
        const dirty = new Set(this.dirtyRoles());
        dirty.delete(role.id);
        this.dirtyRoles.set(dirty);
        this.busy.set(false);
        // Eigene Permissions ggf. aktualisieren (falls Admin sich selbst betroffen hat).
        this.permissions.load().subscribe();
      },
      error: (err) => {
        this.busy.set(false);
        this.saveError.set(this.extractError(err, 'Speichern fehlgeschlagen.'));
      },
    });
  }

  saveAllDirty(): void {
    const dirtyIds = Array.from(this.dirtyRoles());
    if (dirtyIds.length === 0) return;
    this.busy.set(true);
    this.saveError.set(null);
    const calls = dirtyIds.map((rid) =>
      this.featureService.setForRole(rid, Array.from(this.assignments().get(rid) ?? [])),
    );
    forkJoin(calls).subscribe({
      next: (results) => {
        const map = new Map(this.assignments());
        dirtyIds.forEach((rid, i) => {
          map.set(rid, new Set(results[i].featureIds ?? []));
        });
        this.assignments.set(map);
        this.dirtyRoles.set(new Set());
        this.busy.set(false);
        this.permissions.load().subscribe();
      },
      error: (err) => {
        this.busy.set(false);
        this.saveError.set(this.extractError(err, 'Speichern fehlgeschlagen.'));
      },
    });
  }

  // --- Feature CRUD ---

  openCreate(): void {
    this.editingFeature.set(null);
    this.featureFormOpen.set(true);
  }

  openEdit(feature: Feature): void {
    this.editingFeature.set(feature);
    this.featureFormOpen.set(true);
  }

  closeFeatureForm(): void {
    this.featureFormOpen.set(false);
    this.editingFeature.set(null);
  }

  onFeatureSubmit(req: FeatureRequest): void {
    const current = this.editingFeature();
    this.busy.set(true);
    this.saveError.set(null);
    const obs = current
      ? this.featureService.update(current.id, req)
      : this.featureService.create(req);
    obs.subscribe({
      next: (feature) => {
        this.busy.set(false);
        this.closeFeatureForm();
        if (current) {
          this.features.update((list) => list.map((f) => (f.id === feature.id ? feature : f)));
        } else {
          this.features.update((list) => [...list, feature]);
        }
      },
      error: (err) => {
        this.busy.set(false);
        this.saveError.set(this.extractError(err, 'Feature konnte nicht gespeichert werden.'));
      },
    });
  }

  askDeleteFeature(feature: Feature): void {
    this.confirmDeleteFeature.set(feature);
  }

  cancelDeleteFeature(): void {
    this.confirmDeleteFeature.set(null);
  }

  confirmDeleteFeatureExec(): void {
    const f = this.confirmDeleteFeature();
    if (!f) return;
    this.busy.set(true);
    this.featureService.remove(f.id).subscribe({
      next: () => {
        this.features.update((list) => list.filter((x) => x.id !== f.id));
        const map = new Map(this.assignments());
        for (const [rid, set] of map) {
          if (set.has(f.id)) {
            const next = new Set(set);
            next.delete(f.id);
            map.set(rid, next);
          }
        }
        this.assignments.set(map);
        this.confirmDeleteFeature.set(null);
        this.busy.set(false);
      },
      error: (err) => {
        this.busy.set(false);
        this.saveError.set(this.extractError(err, 'Feature konnte nicht gelöscht werden.'));
        this.confirmDeleteFeature.set(null);
      },
    });
  }

  private extractError(err: unknown, fallback: string): string {
    const e = err as { error?: unknown; message?: string };
    if (typeof e?.error === 'string' && e.error.length > 0) return e.error;
    if (typeof e?.message === 'string' && e.message.length > 0) return e.message;
    return fallback;
  }
}
