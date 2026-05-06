import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RoleService } from '../role.service';
import { Role } from '../role.model';

@Component({
  selector: 'app-roles-list',
  imports: [RouterLink],
  templateUrl: './roles-list.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RolesListComponent {
  private readonly service = inject(RoleService);

  readonly roles = signal<Role[]>([]);
  readonly loadError = signal<string | null>(null);
  readonly actionError = signal<string | null>(null);
  readonly confirmDeleteId = signal<number | null>(null);
  readonly busy = signal(false);

  constructor() {
    this.reload();
  }

  reload(): void {
    this.service
      .list(true)
      .pipe(takeUntilDestroyed())
      .subscribe({
        next: (roles) => {
          this.roles.set(roles);
          this.loadError.set(null);
        },
        error: () => this.loadError.set('Rollen konnten nicht geladen werden.'),
      });
  }

  askDelete(id: number): void {
    this.actionError.set(null);
    this.confirmDeleteId.set(id);
  }

  cancelDelete(): void {
    this.confirmDeleteId.set(null);
  }

  confirmDelete(): void {
    const id = this.confirmDeleteId();
    if (id == null) return;
    this.busy.set(true);
    this.service.remove(id).subscribe({
      next: () => {
        this.busy.set(false);
        this.confirmDeleteId.set(null);
        this.roles.update((list) => list.filter((r) => r.id !== id));
      },
      error: (err) => {
        this.busy.set(false);
        this.actionError.set(
          typeof err?.error === 'string' && err.error.length > 0
            ? err.error
            : 'Rolle konnte nicht gelöscht werden.',
        );
        this.confirmDeleteId.set(null);
      },
    });
  }
}
